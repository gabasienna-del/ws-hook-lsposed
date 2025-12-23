package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        if (!lpparam.packageName.equals("kz.asemainala.app")
                && !lpparam.packageName.equals("sinet.startup.inDriver")) {
            return;
        }

        XposedBridge.log("🔥 HTTP/WS HOOK loaded for " + lpparam.packageName);

        /* =========================
           WebSocket connect hook
           ========================= */
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.OkHttpClient",
                    lpparam.classLoader,
                    "newWebSocket",
                    Request.class,
                    WebSocketListener.class,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            Request req = (Request) param.args[0];
                            WebSocketListener listener = (WebSocketListener) param.args[1];

                            try {
                                XposedBridge.log("🌐 WS CONNECT → " + req.url());
                            } catch (Throwable ignored) {}

                            hookWebSocketListener(listener);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ WS hook error: " + t);
        }
    }

    /* =========================
       WebSocket listener hooks
       ========================= */
    private void hookWebSocketListener(Object listener) {

        if (listener == null) return;

        Class<?> cls = listener.getClass();
        XposedBridge.log("👂 WS Listener class → " + cls.getName());

        /* -------- TEXT MESSAGE -------- */
        try {
            XposedHelpers.findAndHookMethod(
                    cls,
                    "onMessage",
                    WebSocket.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[1];
                            XposedBridge.log("📩 WS TEXT: " + msg);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("⚠️ WS text hook skip: " + t);
        }

        /* -------- BINARY MESSAGE (Ls2/m) -------- */
        try {
            XposedHelpers.findAndHookMethod(
                    cls,
                    "onMessage",
                    WebSocket.class,
                    Object.class, // ⚠️ НЕ okio.ByteString
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object bs = param.args[1];
                                byte[] data = (byte[]) XposedHelpers.callMethod(bs, "toByteArray");
                                XposedBridge.log("📦 WS BYTES len=" + data.length);
                            } catch (Throwable t) {
                                XposedBridge.log("❌ WS bytes error: " + t);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("⚠️ WS binary hook skip: " + t);
        }
    }
}
