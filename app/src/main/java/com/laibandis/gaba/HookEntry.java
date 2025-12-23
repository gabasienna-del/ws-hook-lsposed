package com.laibandis.gaba;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + TARGET);

        hookWebSocketSend(lpparam);
        hookWebSocketListener(lpparam);
    }

    /* ===============================
       WebSocket SEND (text + binary)
       =============================== */
    private void hookWebSocketSend(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wsClass = XposedHelpers.findClass(
                    "okhttp3.RealWebSocket",
                    lpparam.classLoader
            );

            // send(String)
            XposedHelpers.findAndHookMethod(
                    wsClass,
                    "send",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[0];
                            XposedBridge.log("📤 WS SEND TEXT → " + msg);
                        }
                    }
            );

            // send(ByteString)
            XposedHelpers.findAndHookMethod(
                    wsClass,
                    "send",
                    XposedHelpers.findClass("okio.ByteString", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object bs = param.args[0];
                            byte[] data = (byte[]) XposedHelpers.callMethod(bs, "toByteArray");
                            XposedBridge.log("📤 WS SEND BIN → " + bytesToHex(data));
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ WS SEND hook failed: " + t);
        }
    }

    /* ===============================
       WebSocket RECEIVE (AUTH HERE)
       =============================== */
    private void hookWebSocketListener(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> listener = XposedHelpers.findClass(
                    "okhttp3.WebSocketListener",
                    lpparam.classLoader
            );

            // onMessage(String)
            XposedHelpers.findAndHookMethod(
                    listener,
                    "onMessage",
                    Object.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[1];
                            XposedBridge.log("📥 WS RECV TEXT → " + msg);
                        }
                    }
            );

            // onMessage(ByteString)
            XposedHelpers.findAndHookMethod(
                    listener,
                    "onMessage",
                    Object.class,
                    XposedHelpers.findClass("okio.ByteString", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object bs = param.args[1];
                            byte[] data = (byte[]) XposedHelpers.callMethod(bs, "toByteArray");
                            XposedBridge.log("📥 WS RECV BIN → " + bytesToHex(data));
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ WS LISTENER hook failed: " + t);
        }
    }

    /* ===============================
       Utils
       =============================== */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
