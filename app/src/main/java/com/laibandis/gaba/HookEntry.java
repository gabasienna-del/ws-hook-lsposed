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

        XposedBridge.log("🔥 HTTP-HOOK loaded for " + lpparam.packageName);

        hookRealInterceptorChain(lpparam);
        hookWebSocket(lpparam);
    }

    /* =========================
       OkHttp HTTP hook
       ========================= */
    private void hookRealInterceptorChain(XC_LoadPackage.LoadPackageParam lpparam) {
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

                                XposedBridge.log("🔥 HTTP-HOOK ▶ URL: " + url);
                                XposedBridge.log("🔥 HTTP-HOOK ▶ HEADERS:\n" + headers);
                            } catch (Throwable t) {
                                XposedBridge.log("❌ HTTP request log error: " + t);
                            }
                        }
                    }
            );

            XposedBridge.log("🔥 HTTP-HOOK RealInterceptorChain hooked OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ HTTP hook error: " + t);
        }
    }

    /* =========================
       WebSocket hook
       ========================= */
    private void hookWebSocket(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> wsClass = XposedHelpers.findClass(
                    "okhttp3.internal.ws.RealWebSocket",
                    lpparam.classLoader
            );

            // TEXT messages
            XposedHelpers.findAndHookMethod(
                    wsClass,
                    "onMessage",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[0];
                            if (msg != null && msg.length() > 2) {
                                XposedBridge.log("🔥 WS ◀ STRING:\n" + msg);
                            }
                        }
                    }
            );

            // BINARY messages
            XposedHelpers.findAndHookMethod(
                    wsClass,
                    "onMessage",
                    XposedHelpers.findClass("okio.ByteString", lpparam.classLoader),
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object bs = param.args[0];
                                byte[] data = (byte[]) XposedHelpers.callMethod(bs, "toByteArray");
                                String text = new String(data);
                                if (!text.isEmpty()) {
                                    XposedBridge.log("🔥 WS ◀ BYTES:\n" + text);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("❌ WS bytes decode error: " + t);
                            }
                        }
                    }
            );

            XposedBridge.log("🔥 WS-HOOK RealWebSocket.onMessage hooked");

        } catch (Throwable t) {
            XposedBridge.log("❌ WS hook error: " + t);
        }
    }
}
