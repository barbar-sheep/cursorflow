package com.cursorflow.effect;

import javafx.scene.canvas.GraphicsContext;

import java.util.Map;

/**
 * 鼠标拖尾特效接口：所有特效必须实现此接口
 */
public interface ITrailEffect {
    /**
     * 初始化特效（加载配置）
     * @param config 特效配置（颜色、速度、长度等）
     */
    void init(Map<String, Object> config);

    /**
     * 渲染特效
     * @param gc Canvas绘图上下文
     * @param mouseX 当前鼠标X坐标
     * @param mouseY 当前鼠标Y坐标
     */
    void render(GraphicsContext gc, int mouseX, int mouseY);

    /**
     * 销毁特效（释放资源）
     */
    void dispose();
}