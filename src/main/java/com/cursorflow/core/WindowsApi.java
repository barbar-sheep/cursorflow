package com.cursorflow.core;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

/**
 * Windows API封装：实现鼠标穿透、窗口属性修改
 */
public class WindowsApi {
    // 扩展窗口样式：透明（鼠标穿透）
    private static final int WS_EX_TRANSPARENT = 0x00000020;
    // 扩展窗口样式：顶层窗口
    private static final int WS_EX_TOPMOST = 0x00000008;

    // 不需要再定义 CustomUser32 接口，直接使用 User32.INSTANCE 即可

    /**
     * 启用窗口鼠标穿透
     * @param stageJavaFX JavaFX窗口
     */
    public static void enableMousePassthrough(javafx.stage.Stage stageJavaFX) {
        // 1. 获取窗口句柄
        WinDef.HWND hwnd = getHWND(stageJavaFX);
        if (hwnd == null) {
            System.err.println("未找到窗口句柄");
            return;
        }

        // 2. 获取当前样式 (返回的是 LONG_PTR 对象，需转为 long)
        BaseTSD.LONG_PTR longPtr = User32.INSTANCE.GetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE);
        long exStyle = longPtr.longValue();

        // 3. 添加"透明+顶层"样式
        exStyle |= WS_EX_TRANSPARENT | WS_EX_TOPMOST;

        // 4. 设置新样式 (需将 long 封装回 LONG_PTR)
        User32.INSTANCE.SetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE, new BaseTSD.LONG_PTR(exStyle).toPointer());
    }

    /**
     * 禁用鼠标穿透
     * @param stageJavaFX JavaFX窗口
     */
    public static void disableMousePassthrough(javafx.stage.Stage stageJavaFX) {
        WinDef.HWND hwnd = getHWND(stageJavaFX);
        if (hwnd == null) return;

        BaseTSD.LONG_PTR longPtr = User32.INSTANCE.GetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE);
        long exStyle = longPtr.longValue();

        // 移除透明样式
        exStyle &= ~WS_EX_TRANSPARENT;

        User32.INSTANCE.SetWindowLongPtr(hwnd, WinUser.GWL_EXSTYLE, new BaseTSD.LONG_PTR(exStyle).toPointer());
    }

    /**
     * 将JavaFX Stage转为Windows HWND
     * <p>注意：JavaFX Stage 对象本身并没有 'hwnd' 字段。
     * 此处使用 FindWindow 根据标题查找最为通用。</p>
     */
    private static WinDef.HWND getHWND(javafx.stage.Stage stage) {
        try {
            // 通过窗口标题查找（简单通用）
            // 确保你的 Stage 设置了唯一的 Title
            String title = stage.getTitle();
            if (title != null && !title.isEmpty()) {
                return User32.INSTANCE.FindWindow(null, title);
            }
            return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}