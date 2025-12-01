package com.cursorflow;

import com.cursorflow.core.OverlayManager;
import com.cursorflow.core.TrayIconManager;
import com.cursorflow.effect.EffectFactory;
import javafx.application.Application;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 程序入口
 */
public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {
            // 初始化全局图层管理器
            OverlayManager overlayManager = OverlayManager.getInstance();

            // 初始化系统托盘
            new TrayIconManager(primaryStage);

            // 配置 Python 驱动特效（使用绝对路径，避免错误）
            String userConfigPath = System.getProperty("user.dir")
                    + File.separator + "src"
                    + File.separator + "main"
                    + File.separator + "python"
                    + File.separator + "user_configs"
                    + File.separator + "my_particle_effect.py";
            Map<String, Object> pythonConfig = new HashMap<>();
            pythonConfig.put("configFilePath", userConfigPath);

            // 尝试创建 Python 特效（增加容错）
            var pythonEffect = EffectFactory.createEffect("python_driven", pythonConfig);
            overlayManager.switchEffect(pythonEffect);

            // 隐藏主窗口
            primaryStage.hide();
        } catch (Exception e) {
            // 初始化失败时显示弹窗提示
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("初始化失败");
            alert.setHeaderText("J-CursorFlow 启动失败");
            alert.setContentText("原因：" + e.getMessage() + "\n请检查 Python 环境和配置文件路径");
            alert.showAndWait();
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}