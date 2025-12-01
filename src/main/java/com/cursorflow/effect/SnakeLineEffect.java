package com.cursorflow.effect;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * 贪吃蛇线条特效：鼠标轨迹为连续线条（定长队列实现）
 */
public class SnakeLineEffect implements ITrailEffect {
    // 轨迹点队列（定长，保证线条长度）
    private Queue<Point> trailPoints = new LinkedList<>();
    // 最大轨迹长度
    private int maxLength = 30;
    // 线条颜色
    private Color lineColor = Color.PURPLE;
    // 线条宽度
    private double lineWidth = 3.0;

    @Override
    public void init(Map<String, Object> config) {
        // 从配置中读取参数（默认值兜底）
        if (config != null) {
            maxLength = config.getOrDefault("maxLength", 30) instanceof Integer ? (Integer) config.get("maxLength") : 30;
            lineWidth = config.getOrDefault("lineWidth", 3.0) instanceof Double ? (Double) config.get("lineWidth") : 3.0;
            lineColor = config.getOrDefault("color", "#9C27B0") instanceof String ? Color.web((String) config.get("color")) : Color.PURPLE;
        }
    }

    @Override
    public void render(GraphicsContext gc, int mouseX, int mouseY) {
        // 添加当前鼠标位置到队列
        trailPoints.add(new Point(mouseX, mouseY));

        // 超过最大长度则移除最早的点
        if (trailPoints.size() > maxLength) {
            trailPoints.poll();
        }

        // 绘制线条
        gc.setStroke(lineColor);
        gc.setLineWidth(lineWidth);
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND); // 线条端点圆润

        gc.beginPath();
        Point first = trailPoints.peek();
        if (first != null) {
            gc.moveTo(first.x, first.y);
            // 连接所有轨迹点
            for (Point p : trailPoints) {
                gc.lineTo(p.x, p.y);
            }
            gc.stroke();
        }
    }

    @Override
    public void dispose() {
        trailPoints.clear(); // 释放资源
    }

    // 内部点坐标类
    private static class Point {
        int x;
        int y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}