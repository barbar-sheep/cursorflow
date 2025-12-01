package com.cursorflow.core;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.platform.win32.WinUser.HHOOK;
import com.sun.jna.platform.win32.WinUser.LowLevelMouseProc;
import com.sun.jna.platform.win32.WinUser.MSLLHOOKSTRUCT;

/**
 * 全局鼠标钩子：通过WH_MOUSE_LL获取全局鼠标坐标
 */
public class MouseHook {
    // 鼠标低级别钩子
    private static final int WH_MOUSE_LL = 14;
    // 鼠标移动消息
    private static final int WM_MOUSEMOVE = 0x0200;

    // 钩子句柄
    private HHOOK hookHandle;
    // 鼠标坐标回调
    private MousePositionListener listener;

    // 钩子回调接口（显式实现以确保参数类型正确）
    private final LowLevelMouseProc mouseProc = new LowLevelMouseProc() {
        @Override
        public LRESULT callback(int nCode, WPARAM wParam, MSLLHOOKSTRUCT info) {
            if (nCode >= 0 && wParam.intValue() == WM_MOUSEMOVE) {
                if (info != null) {
                    int x = info.pt.x;
                    int y = info.pt.y;

                    // 回调给监听器
                    if (listener != null) {
                        listener.onMouseMove(x, y);
                    }
                }
            }
            // 传递钩子消息（避免影响系统）
            // CallNextHookEx 期望的是 LPARAM 类型；将结构体指针转换为 LPARAM
            LPARAM lParam = null;
            if (info != null && info.getPointer() != null) {
                lParam = new LPARAM(Pointer.nativeValue(info.getPointer()));
            } else {
                lParam = new LPARAM(0);
            }
            return User32.INSTANCE.CallNextHookEx(hookHandle, nCode, wParam, lParam);
        }
    };

    /**
     * 启动鼠标钩子
     */
    public void start(MousePositionListener listener) {
        this.listener = listener;
        // 安装低级别鼠标钩子
        hookHandle = User32.INSTANCE.SetWindowsHookEx(
                WH_MOUSE_LL,
                mouseProc,
                Kernel32.INSTANCE.GetModuleHandle(null),
                0 // 全局钩子（所有线程）
        );

        if (hookHandle == null) {
            throw new RuntimeException("安装鼠标钩子失败：" + Kernel32.INSTANCE.GetLastError());
        }

        // 启动消息循环（必须，否则钩子不生效）
        new Thread(() -> {
            WinUser.MSG msg = new WinUser.MSG();
            while (User32.INSTANCE.GetMessage(msg, null, 0, 0) > 0) {
                User32.INSTANCE.TranslateMessage(msg);
                User32.INSTANCE.DispatchMessage(msg);
            }
        }).start();
    }

    /**
     * 停止鼠标钩子
     */
    public void stop() {
        if (hookHandle != null) {
            User32.INSTANCE.UnhookWindowsHookEx(hookHandle);
            hookHandle = null;
        }
    }

    /**
     * 鼠标坐标监听器接口
     */
    public interface MousePositionListener {
        void onMouseMove(int x, int y);
    }
}