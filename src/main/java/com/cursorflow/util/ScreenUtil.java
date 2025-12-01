package com.cursorflow.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.util.List;

/**
 * 屏幕工具类：获取屏幕尺寸
 */
public class ScreenUtil {
    /**
     * 获取所有屏幕的合并边界（适配多屏扩展）
     */
    public static Rectangle2D getTotalScreenBounds() {
        List<Screen> screens = Screen.getScreens();
        if (screens.isEmpty()) {
            return new Rectangle2D(0, 0, 1920, 1080); // 默认分辨率
        }

        // 计算所有屏幕的最小X、最小Y、最大X、最大Y
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Screen screen : screens) {
            Rectangle2D bounds = screen.getBounds();
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }

        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
}