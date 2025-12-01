package com.cursorflow.effect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.cursorflow.core.PythonProcessManager;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Python 驱动的特效：解析 Python 渲染指令并绘制
 */
public class PythonDrivenEffect implements ITrailEffect {
    private final Gson gson = new Gson();
    private String lastRenderCommand; // 缓存最后一条渲染指令
    private final PythonProcessManager pythonManager;

    public PythonDrivenEffect(String configFilePath) {
        // 初始化 Python 进程管理器
        this.pythonManager = new PythonProcessManager(configFilePath);
        // 设置渲染指令回调
        this.pythonManager.setRenderListener(command -> lastRenderCommand = command);
        // 启动 Python 进程
        this.pythonManager.startProcess();
    }

    @Override
    public void init(Map<String, Object> config) {
        // 配置由 Python 读取，此处无需处理
    }

    @Override
    public void render(GraphicsContext gc, int mouseX, int mouseY) {
        // 向 Python 发送当前鼠标坐标
        pythonManager.sendMousePosition(mouseX, mouseY);

        // 如果没有渲染指令，直接返回
        if (lastRenderCommand == null || lastRenderCommand.isEmpty()) return;

        try {
            // 解析 Python 发送的渲染指令
            JsonObject commandJson = gson.fromJson(lastRenderCommand, JsonObject.class);

            // --- 核心修复开始：防御性检查 ---
            // 检查 JSON 是否为空，或者是否缺少关键的 "type" 字段
            if (commandJson == null || !commandJson.has("type")) {
                // 如果收到的数据不合法，暂时忽略本次渲染（防止 NPE 崩溃）
                return;
            }
            // --- 核心修复结束 ---

            String effectType = commandJson.get("type").getAsString();

            // 根据特效类型绘制
            switch (effectType) {
                case "particle":
                    // 再次检查关键数据是否存在
                    if (commandJson.has("particles")) {
                        renderParticles(gc, commandJson);
                    }
                    break;
                case "snake_line":
                    if (commandJson.has("points")) {
                        renderSnakeLine(gc, commandJson);
                    }
                    break;
                case "sprite":
                    renderSprite(gc, commandJson);
                    break;
                default:
                    // 未知类型清除画布
                    gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());
            }
        } catch (Exception e) {
            // 捕获 JsonSyntaxException 等其他潜在错误，打印日志但不要崩溃
            System.err.println("渲染指令解析错误的: " + e.getMessage());
            // 可选：e.printStackTrace();
        }
    }

    /**
     * 绘制粒子特效
     */
    private void renderParticles(GraphicsContext gc, JsonObject commandJson) {
        // 清除画布
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // 获取粒子数据
        // 注意：全局 opacity 已经集成在 python 的 "alpha" 计算里了，这里不需要再读 commandJson.get("opacity")
        if (!commandJson.has("particles")) return;

        var particlesArray = commandJson.get("particles").getAsJsonArray();

        // 绘制每个粒子
        for (var pElement : particlesArray) {
            JsonObject particle = pElement.getAsJsonObject();

            double x = particle.get("x").getAsDouble();
            double y = particle.get("y").getAsDouble();
            double size = particle.get("size").getAsDouble();
            String colorStr = particle.get("color").getAsString();

            // 【关键修改】：读取每个粒子的透明度 alpha
            double alpha = 1.0;
            if (particle.has("alpha")) {
                alpha = particle.get("alpha").getAsDouble();
            }

            // 使用带透明度的颜色
            // 确保 alpha 在 0.0 - 1.0 之间
            alpha = Math.max(0, Math.min(1, alpha));

            Color color = Color.web(colorStr, alpha); // 第二个参数就是 Opacity
            gc.setFill(color);

            // 绘制
            gc.fillOval(x - size / 2, y - size / 2, size, size);
        }
    }

    /**
     * 绘制线条特效
     */
    private void renderSnakeLine(GraphicsContext gc, JsonObject commandJson) {
        // 清除画布
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // 获取线条参数
        double opacity = commandJson.get("opacity").getAsDouble();
        String colorStr = commandJson.get("color").getAsString();
        double width = commandJson.get("width").getAsDouble();
        boolean roundCap = commandJson.get("round_cap").getAsBoolean();
        boolean fadeOut = commandJson.get("fade_out").getAsBoolean();
        List<Map<String, Object>> points = gson.fromJson(
                commandJson.get("points"),
                List.class
        );

        // 设置线条样式
        Color color = Color.web(colorStr, opacity);
        gc.setStroke(color);
        gc.setLineWidth(width);
        gc.setLineCap(roundCap ? StrokeLineCap.ROUND : StrokeLineCap.BUTT);

        // 绘制线条
        if (points.size() >= 2) {
            gc.beginPath();
            Map<String, Object> firstPoint = points.get(0);
            gc.moveTo(
                    ((Number) firstPoint.get("x")).doubleValue(),
                    ((Number) firstPoint.get("y")).doubleValue()
            );

            // 渐变透明度（如果启用）
            double totalPoints = points.size();
            for (int i = 1; i < points.size(); i++) {
                Map<String, Object> point = points.get(i);
                if (fadeOut) {
                    // 后面的点透明度逐渐降低
                    double alpha = opacity * (i / totalPoints);
                    gc.setStroke(Color.web(colorStr, alpha));
                }
                gc.lineTo(
                        ((Number) point.get("x")).doubleValue(),
                        ((Number) point.get("y")).doubleValue()
                );
            }
            gc.stroke();
        }
    }

    /**
     * 绘制贴图特效
     */
    private void renderSprite(GraphicsContext gc, JsonObject commandJson) {
        // 清除画布
        gc.clearRect(0, 0, gc.getCanvas().getWidth(), gc.getCanvas().getHeight());

        // 获取贴图参数
        double opacity = commandJson.get("opacity").getAsDouble();
        double x = commandJson.get("x").getAsDouble();
        double y = commandJson.get("y").getAsDouble();
        double width = commandJson.get("width").getAsDouble();
        double height = commandJson.get("height").getAsDouble();
        String imagePath = commandJson.get("image_path").getAsString();
        double rotate = commandJson.get("rotate").getAsDouble();

        // 加载贴图（此处简化，实际需缓存图片资源）
        javafx.scene.image.Image image = new javafx.scene.image.Image(
                new File(imagePath).toURI().toString(),
                width, height, true, true
        );

        // 设置透明度
        gc.setGlobalAlpha(opacity);

        // 绘制贴图（支持旋转）
        if (rotate != 0) {
            gc.save();
            gc.translate(x + width / 2, y + height / 2);
            gc.rotate(rotate);
            gc.drawImage(image, -width / 2, -height / 2, width, height);
            gc.restore();
        } else {
            gc.drawImage(image, x, y, width, height);
        }

        // 重置透明度
        gc.setGlobalAlpha(1.0);
    }

    @Override
    public void dispose() {
        // 停止 Python 进程
        pythonManager.stopProcess();
        lastRenderCommand = null;
    }
}