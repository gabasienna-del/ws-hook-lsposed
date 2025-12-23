package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String[] TARGETS = {
            "kz.asemainala.app",
            "sinet.startup.inDriver"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!isTarget(lpparam.packageName)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + lpparam.packageName);

        hookWebSocketSend(lpparam);
        hookWebSocketListener(lpparam);
    }

    private boolean isTarget(String pkg) {
        for (String t : TARGETS) {
            if (t.equals(pkg)) return true;
        }
        return false;
    }

    /* ===============================
       WebSocket SEND
       =============================== */
    private void hookWebSocketSend(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> ws = XposedHelpers.findClass(
                    "okhttp3.RealWebSocket",
                    lpparam.classLoader
            );

            // send(String)
            XposedHelpers.findAndHookMethod(
                    ws,
                    "send",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            XposedBridge.log("📤 [" + lpparam.packageName + "] WS SEND TEXT → "
                                    + param.args[0]);
                        }
                    }
            );

            // send(ByteString)
            XposedHelpers.findAndHookMethod(
                    ws,
                    "send",
                    XposedHelpers.findClass("okio.ByteString", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Object bs = param.args[0];
                            byte[] data = (byte[]) XposedHelpers.callMethod(bs, "toByteArray");
                            XposedBridge.log("📤 [" + lpparam.packageName + "] WS SEND BIN → "
                                    + bytesToHex(data));
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
                            XposedBridge.log("📥 [" + lpparam.packageName + "] WS RECV TEXT → "
                                    + param.args[1]);
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
                            XposedBridge.log("📥 [" + lpparam.packageName + "] WS RECV BIN → "
                                    + bytesToHex(data));
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
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
