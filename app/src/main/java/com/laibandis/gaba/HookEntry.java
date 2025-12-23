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
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        boolean target = false;
        for (String pkg : TARGETS) {
            if (pkg.equals(lpparam.packageName)) {
                target = true;
                break;
            }
        }
        if (!target) return;

        XposedBridge.log("🔥 HTTP/WS HOOK loaded for " + lpparam.packageName);

        hookHttp(lpparam);
        hookWebSocketListener(lpparam);
    }

    /* =========================
       HTTP (OkHttp)
       ========================= */
    private void hookHttp(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> chainCls = XposedHelpers.findClass(
                    "okhttp3.internal.http.RealInterceptorChain",
                    lpparam.classLoader
            );

            XposedHelpers.findAndHookMethod(
                    chainCls,
                    "proceed",
                    XposedHelpers.findClass("okhttp3.Request", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object req = param.args[0];
                                Object url = XposedHelpers.callMethod(req, "url");
                                Object headers = XposedHelpers.callMethod(req, "headers");

                                XposedBridge.log("🔥 HTTP ▶ URL: " + url);
                                XposedBridge.log("🔥 HTTP ▶ HEADERS:\n" + headers);
                            } catch (Throwable t) {
                                XposedBridge.log("❌ HTTP log error: " + t);
                            }
                        }
                    }
            );

            XposedBridge.log("🔥 HTTP hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ HTTP hook failed: " + t);
        }
    }

    /* =========================
       WebSocket (OkHttp 4.x)
       ========================= */
    private void hookWebSocketListener(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> listenerCls = XposedHelpers.findClass(
                    "okhttp3.WebSocketListener",
                    lpparam.classLoader
            );

            // TEXT
            XposedHelpers.findAndHookMethod(
                    listenerCls,
                    "onMessage",
                    XposedHelpers.findClass("okhttp3.WebSocket", lpparam.classLoader),
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[1];
                            if (msg != null && !msg.isEmpty()) {
                                XposedBridge.log("🔥 WS ◀ TEXT:\n" + msg);
                            }
                        }
                    }
            );

            // BINARY
            XposedHelpers.findAndHookMethod(
                    listenerCls,
                    "onMessage",
                    XposedHelpers.findClass("okhttp3.WebSocket", lpparam.classLoader),
                    XposedHelpers.findClass("okio.ByteString", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object bs = param.args[1];
                                byte[] data = (byte[]) XposedHelpers.callMethod(bs, "toByteArray");
                                String text = new String(data);
                                if (!text.isEmpty()) {
                                    XposedBridge.log("🔥 WS ◀ BYTES:\n" + text);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("❌ WS binary decode error: " + t);
                            }
                        }
                    }
            );

            XposedBridge.log("🔥 WS listener hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ WS hook failed: " + t);
        }
    }
}
