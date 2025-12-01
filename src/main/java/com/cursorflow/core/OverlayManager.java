package com.cursorflow.core;

import com.cursorflow.effect.ITrailEffect;
import com.cursorflow.util.ScreenUtil;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.awt.MouseInfo;
import java.awt.Point;

/**
 * 全局图层管理器（单例）：管理全屏穿透图层、渲染循环、特效切换
 *
 * 修正点：
 * 1. 移除了 MouseHook，改用原生 MouseInfo 轮询，确保坐标绝对能获取到。
 * 2. 移除了 gc.clearRect，将清空控制权交给 Effect，避免闪烁或残影丢失。
 */
public class OverlayManager {
    // 单例实例
    private static OverlayManager INSTANCE;

    private Stage overlayStage;
    private Canvas canvas;
    private GraphicsContext gc;
    private ITrailEffect currentEffect;
    private AnimationTimer renderTimer;

    // 分辨率缩放因子 (处理 Windows 125%/150% 缩放)
    private double scaleX = 1.0;
    private double scaleY = 1.0;

    private OverlayManager() {
        initOverlayStage();
        initRenderTimer();
        // 废弃 initMouseHook()，改在 renderTimer 中轮询
    }

    public static synchronized OverlayManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new OverlayManager();
        }
        return INSTANCE;
    }

    private void initOverlayStage() {
        overlayStage = new Stage();
        overlayStage.initStyle(StageStyle.TRANSPARENT);
        overlayStage.setAlwaysOnTop(true);
        // 不使用 FullScreen(true)，因为那会独占焦点。
        // 使用手动设置大小来覆盖全屏
        overlayStage.setX(0);
        overlayStage.setY(0);

        // 如果你有 ScreenUtil 可以用，否则建议直接使用 JavaFX Screen 获取大小
        // var screenBounds = ScreenUtil.getTotalScreenBounds();
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();

        // 注意：canvas 的尺寸应与屏幕像素对齐
        canvas = new Canvas(bounds.getWidth(), bounds.getHeight());
        gc = canvas.getGraphicsContext2D();

        // 简单的屏幕缩放计算 (JavaFX 会自动处理 DPI，但 AWT MouseInfo 是物理像素)
        // 通常不需要手动除 scale，但在多屏异构缩放时可能需要微调
        // 这里暂时保持 1:1，因为 JavaFX 自动映射逻辑通常够用

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: transparent;");

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        overlayStage.setScene(scene);

        // 标题，方便 Spy++ 等工具调试查找
        overlayStage.setTitle("CursorFlow-Overlay");

        // 必须先 show，WindowsAPI 才能找到句柄
        overlayStage.show();
        WindowsApi.enableMousePassthrough(overlayStage);
    }

    private void initRenderTimer() {
        renderTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 如果没有特效，不消耗资源
                if (currentEffect == null) return;

                // --- 修正 1: 主动轮询获取鼠标位置 (比 MouseHook 更稳定) ---
                Point p = MouseInfo.getPointerInfo().getLocation();
                int rawX = (int) p.getX();
                int rawY = (int) p.getY();

                // --- 修正 2: 移除 clearRect，让 Effect 自己决定是否清空 ---
                // gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

                // 渲染：将坐标传给 Python
                // 对于 JavaFX Canvas，坐标系通常已自动缩放，无需手动除 DPI
                // 如果发现鼠标错位，这里可以除以 screenScale
                currentEffect.render(gc, rawX, rawY);
            }
        };
        renderTimer.start();
    }

    public void switchEffect(ITrailEffect effect) {
        if (currentEffect != null) {
            currentEffect.dispose();
        }
        currentEffect = effect;

        // 切换特效时，如果不清空可能会残留上一特效的画面
        if (gc != null && canvas != null) {
            gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }

        // 确保切换特效后窗口还是顶置显示的
        if (!overlayStage.isShowing()) {
            show();
        }
    }

    public void show() {
        if (!overlayStage.isShowing()) {
            overlayStage.show();
            // 每次 show 可能需要重新设置穿透
            WindowsApi.enableMousePassthrough(overlayStage);
        }
    }

    public void hide() {
        if (overlayStage.isShowing()) {
            overlayStage.hide();
        }
    }

    public void exit() {
        if (renderTimer != null) renderTimer.stop();
        if (currentEffect != null) {
            currentEffect.dispose();
        }
        overlayStage.close();
        Platform.exit();
        System.exit(0);
    }
}