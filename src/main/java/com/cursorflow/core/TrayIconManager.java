package com.cursorflow.core;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * 系统托盘管理器：提供托盘菜单
 */
public class TrayIconManager {
    private final TrayIcon trayIcon;
    private final OverlayManager overlayManager;
    private boolean isEffectPaused = false;

    public TrayIconManager(Stage primaryStage) {
        this.overlayManager = OverlayManager.getInstance();

        // 初始化系统托盘（需检查系统是否支持）
        if (!SystemTray.isSupported()) {
            System.err.println("系统不支持托盘功能");
            trayIcon = null;
            return;
        }

        // 创建托盘图标（使用JavaFX资源）
        URL iconUrl = getClass().getResource("/com/cursorflow/icon.png");
        java.awt.Image awtImage = null;
        try {
            if (iconUrl != null) {
                // 直接读取为 AWT Image，无需转换
                awtImage = ImageIO.read(iconUrl);
            } else {
                System.err.println("找不到托盘图标资源");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建托盘菜单
        PopupMenu popupMenu = new PopupMenu();

        // 切换特效菜单（示例：切换线条/粒子特效）
        MenuItem snakeLineItem = new MenuItem("贪吃蛇线条");
        snakeLineItem.addActionListener(e -> Platform.runLater(() -> {
            var effect = com.cursorflow.effect.EffectFactory.createEffect("snake_line", null);
            overlayManager.switchEffect(effect);
            isEffectPaused = false;
        }));

        MenuItem particleItem = new MenuItem("粒子火花");
        particleItem.addActionListener(e -> Platform.runLater(() -> {
            var effect = com.cursorflow.effect.EffectFactory.createEffect("particle", null);
            overlayManager.switchEffect(effect);
            isEffectPaused = false;
        }));

        // 暂停/恢复菜单
        MenuItem pauseItem = new MenuItem("暂停特效");
        pauseItem.addActionListener(e -> {
            isEffectPaused = !isEffectPaused;
            pauseItem.setLabel(isEffectPaused ? "恢复特效" : "暂停特效");
            if (isEffectPaused) {
                overlayManager.hide();
            } else {
                overlayManager.show();
            }
        });

        // 退出菜单
        MenuItem exitItem = new MenuItem("退出");
        exitItem.addActionListener(e -> Platform.runLater(overlayManager::exit));

        // 添加菜单到托盘
        popupMenu.add(snakeLineItem);
        popupMenu.add(particleItem);
        popupMenu.addSeparator();
        popupMenu.add(pauseItem);
        popupMenu.add(exitItem);

        // 创建托盘图标并添加到系统托盘
        trayIcon = new TrayIcon(awtImage, "J-CursorFlow", popupMenu);
        trayIcon.setImageAutoSize(true); // 自动适应图标大小

        SystemTray systemTray = SystemTray.getSystemTray();
        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            e.printStackTrace();
        }

        // 主窗口关闭时隐藏到托盘（不退出程序）
        primaryStage.setOnCloseRequest(e -> {
            e.consume(); // 取消默认关闭行为
            primaryStage.hide();
        });
    }
}