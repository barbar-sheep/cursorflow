package com.cursorflow.core;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Python 进程管理器 (Final Version)
 * 特性：
 * 1. 自动探测 Python 环境 (Win/Mac/Linux)
 * 2. 智能路径解析 (支持 IDE 调试模式与 EXE 打包模式)
 * 3. 崩溃自动重启保护 (带频率限制)
 * 4. 解决 JavaFX 关闭时的 IO 报错与僵尸进程问题
 */
public class PythonProcessManager {
    private static final Logger LOG = LoggerFactory.getLogger(PythonProcessManager.class);

    // 静态路径获取：解决打包后找不到脚本的问题
    private static final String PYTHON_SCRIPT_ABSOLUTE_PATH = getPythonScriptAbsolutePath();

    // 最小重启间隔 (毫秒)
    private static final long MIN_RESTART_INTERVAL = 3000;

    private Process pythonProcess;
    private BufferedReader pythonInput;
    private BufferedWriter pythonOutput;

    private final String configFilePath;
    private WatchService watchService;
    private Consumer<String> renderListener; // 使用标准 Consumer 接口

    // 状态标志位
    private volatile boolean isStarting = false;
    private volatile boolean isManualStop = false; // 关键：标记是否为人为停止
    private long lastRestartTime = 0;

    public PythonProcessManager(String configFilePath) {
        // 标准化路径，确保 Windows 下也能正确读取
        this.configFilePath = standardizeFilePath(configFilePath);
        LOG.info("初始化 Python 管理器 | 脚本: {} | 配置: {}", PYTHON_SCRIPT_ABSOLUTE_PATH, this.configFilePath);
        initConfigFileWatcher();
    }

    /**
     * 设置渲染指令回调
     */
    public void setRenderListener(Consumer<String> listener) {
        this.renderListener = listener;
    }

    /**
     * 启动 Python 进程 (线程安全)
     */
    public synchronized boolean startProcess() {
        if (isStarting) {
            LOG.warn("进程正在启动中，忽略重复请求");
            return false;
        }

        // 频率限制
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastRestartTime < MIN_RESTART_INTERVAL) {
            LOG.warn("重启过于频繁，已忽略。");
            return false;
        }

        // 停止现有进程
        stopProcess(true);

        isStarting = true;
        isManualStop = false; // 重置停止标记

        try {
            // 1. 获取 Python 命令
            String pythonCmd = getPythonCommand();
            if (pythonCmd == null) {
                LOG.error("错误：系统未找到 python 或 python3 命令，请配置环境变量！");
                isStarting = false;
                return false;
            }

            // 2. 校验文件存在性
            if (!new File(PYTHON_SCRIPT_ABSOLUTE_PATH).exists()) {
                LOG.error("严重错误：Python 脚本不存在 -> {}", PYTHON_SCRIPT_ABSOLUTE_PATH);
                isStarting = false;
                return false;
            }

            // 3. 构建进程
            ProcessBuilder pb = new ProcessBuilder(pythonCmd, PYTHON_SCRIPT_ABSOLUTE_PATH, configFilePath);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");
            pb.redirectErrorStream(true); // 将错误输出合并到标准输出，防止缓冲区死锁

            // 设置工作目录为脚本所在目录
            File scriptFile = new File(PYTHON_SCRIPT_ABSOLUTE_PATH);
            pb.directory(scriptFile.getParentFile());

            LOG.info("执行命令: {}", String.join(" ", pb.command()));
            pythonProcess = pb.start();

            // 4. 初始化流 (使用 UTF-8 防止中文乱码)
            pythonInput = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream(), StandardCharsets.UTF_8));
            pythonOutput = new BufferedWriter(new OutputStreamWriter(pythonProcess.getOutputStream(), StandardCharsets.UTF_8));

            // 5. 启动后台线程
            startReadPythonOutputThread();
            startProcessMonitorThread();

            lastRestartTime = System.currentTimeMillis();
            LOG.info("Python 进程启动成功 (PID: {})", pythonProcess.pid());
            isStarting = false;
            return true;

        } catch (IOException e) {
            LOG.error("启动失败: ", e);
            isStarting = false;
            return false;
        }
    }

    /**
     * 发送鼠标位置 (高频调用优化)
     */
    public void sendMousePosition(int x, int y) {
        if (!isProcessAlive() || pythonOutput == null) return;

        try {
            // 直接拼接 JSON 字符串，性能略高于 Gson 序列化
            String json = "{\"x\":" + x + ",\"y\":" + y + "}\n";
            pythonOutput.write(json);
            pythonOutput.flush();
        } catch (IOException e) {
            // 这是一个预期内的异常（如进程突然崩溃），只需记录 debug 级别
            LOG.debug("发送坐标失败，等待监控线程重启进程");
            closeStreams();
        }
    }

    /**
     * 停止进程 (供 PythonDrivenEffect 使用，无参版本)
     */
    public void stopProcess() {
        stopProcess(true);
    }

    /**
     * 停止进程实现
     * @param printLog 是否打印日志
     */
    public synchronized void stopProcess(boolean printLog) {
        // 标记为手动停止，防止监控线程触发重启
        isManualStop = true;

        if (printLog) LOG.info("正在停止 Python 进程...");

        closeStreams();

        if (pythonProcess != null && pythonProcess.isAlive()) {
            try {
                // 优雅关闭：先 destroy，等待 1秒，不行则强杀
                pythonProcess.destroy();
                if (!pythonProcess.waitFor(1000, TimeUnit.MILLISECONDS)) {
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pythonProcess.destroyForcibly();
            }
        }
        pythonProcess = null;
    }

    /**
     * 读取 Python 输出 (日志与指令分离)
     */
    private void startReadPythonOutputThread() {
        Thread thread = new Thread(() -> {
            String line;
            try {
                while (pythonInput != null && (line = pythonInput.readLine()) != null) {
                    // 如果是以 "{" 开头，大概率是 JSON 渲染指令
                    if (line.trim().startsWith("{")) {
                        if (renderListener != null) {
                            String finalLine = line;
                            // 确保渲染在 JavaFX 线程
                            Platform.runLater(() -> renderListener.accept(finalLine));
                        }
                    } else {
                        // 否则视为 Python 脚本的 print() 日志
                        LOG.info("[Py]: {}", line);
                    }
                }
            } catch (IOException e) {
                // 流被关闭属于正常现象
            }
        }, "PyReaderThread");
        thread.setDaemon(true); // 设置为守护线程，随主程序退出
        thread.start();
    }

    /**
     * 进程监控与自动重启
     */
    private void startProcessMonitorThread() {
        Thread thread = new Thread(() -> {
            try {
                if (pythonProcess != null) {
                    int exitCode = pythonProcess.waitFor();

                    // 只有非手动停止，且异常退出时，才重启
                    if (!isManualStop && exitCode != 0) {
                        LOG.warn("Python 进程异常退出 (Code: {})，准备自动重启...", exitCode);
                        restartProcessWithLimit();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "PyMonitorThread");
        thread.setDaemon(true);
        thread.start();
    }

    private void restartProcessWithLimit() {
        new Thread(() -> {
            try {
                Thread.sleep(MIN_RESTART_INTERVAL);
                if (!isManualStop) { // 双重检查
                    startProcess();
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }

    private void closeStreams() {
        try {
            if (pythonOutput != null) pythonOutput.close();
            if (pythonInput != null) pythonInput.close();
        } catch (IOException ignored) {}
        finally {
            pythonOutput = null;
            pythonInput = null;
        }
    }

    private boolean isProcessAlive() {
        return pythonProcess != null && pythonProcess.isAlive();
    }

    // --- 工具方法区域 ---

    private static String getPythonScriptAbsolutePath() {
        try {
            // 1. IDE 环境: src/main/python/main.py
            File dev = new File("src/main/python/main.py");
            if (dev.exists()) return dev.getAbsolutePath();

            // 2. 生产环境: {APP_DIR}/python/main.py
            File prod = new File(System.getProperty("user.dir"), "python/main.py");
            if (prod.exists()) return prod.getAbsolutePath();

            return ""; // 将导致 startProcess 检测失败
        } catch (Exception e) {
            return "";
        }

    }

    private String getPythonCommand() {
        // 简单粗暴的探测逻辑
        String[] commands;
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            commands = new String[]{"python.exe", "python"};
        } else {
            commands = new String[]{"python3", "python"};
        }

        for (String cmd : commands) {
            if (checkCommand(cmd)) return cmd;
        }
        return null;
    }

    private boolean checkCommand(String cmd) {
        try {
            Process p = new ProcessBuilder(cmd, "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 初始化配置文件监听
     * 当检测到 user_configs/*.py 修改时，自动重启进程
     */
    private void initConfigFileWatcher() {
        Thread watcherThread = new Thread(() -> {
            try {
                // 1. 获取要监听的目录
                File configFile = new File(configFilePath);
                File parentDir = configFile.getParentFile();

                // 2. 校验目录是否存在
                if (parentDir == null || !parentDir.exists()) {
                    LOG.warn("配置文件的父目录不存在，跳过文件监听: {}", configFilePath);
                    return;
                }

                // 3. 注册 WatchService
                try (WatchService service = FileSystems.getDefault().newWatchService()) {
                    Path path = parentDir.toPath();
                    // 监听 修改(MODIFY) 和 创建(CREATE，部分编辑器保存机制是 删旧建新)
                    path.register(service, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);

                    LOG.info("配置文件监听器已启动: {}", configFilePath);

                    // 4. 循环等待事件
                    while (!Thread.currentThread().isInterrupted()) {
                        WatchKey key;
                        try {
                            key = service.take(); // 阻塞等待直到有事件发生
                        } catch (InterruptedException x) {
                            return; // 线程中断则退出
                        }

                        boolean needRestart = false;

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent.Kind<?> kind = event.kind();
                            if (kind == StandardWatchEventKinds.OVERFLOW) continue;

                            // 获取变动的文件名
                            Path changedPath = (Path) event.context();

                            // 判断变动的文件是否是我们关注的配置文件
                            if (changedPath.toString().equals(configFile.getName())) {
                                LOG.info("检测到配置文件变化: {}", changedPath);
                                needRestart = true;
                            }
                        }

                        if (needRestart) {
                            // 等待 500ms，确保文件写入完成 (防止读到空文件)
                            try { Thread.sleep(500); } catch (InterruptedException ignored) {}

                            // 调用之前定义好的启动方法（它内部会自动 stop 旧进程）
                            // 且不是手动停止，允许自动重启
                            isManualStop = false;
                            startProcess();
                        }

                        // 重置 Key，若失效则退出循环
                        boolean valid = key.reset();
                        if (!valid) {
                            LOG.warn("监听 Key 失效 (目录可能被删除)，停止监听");
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error("配置文件监听异常", e);
            }
        }, "ConfigWatcher");

        // 设置为守护线程：主程序退出时它会自动结束
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    /**
     * 标准化文件路径
     * 修复：防止对绝对路径重复拼接项目根目录
     */
    private String standardizeFilePath(String path) {
        File file = new File(path);

        // 如果已经是绝对路径，且文件存在，直接返回
        if (file.isAbsolute() && file.exists()) {
            return file.getAbsolutePath().replace("\\", "/");
        }

        // 如果是相对路径，或者是绝对路径但校验失败（防止意外），才尝试拼接 user.dir
        if (!file.exists()) {
            File checkRelative = new File(System.getProperty("user.dir"), path);
            if (checkRelative.exists()) {
                file = checkRelative;
            }
        }

        String finalPath = file.getAbsolutePath().replace("\\", "/");
        LOG.info("路径解析结果: {}", finalPath);
        return finalPath;
    }
}

