package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String[] TARGETS = {
            "kz.asemainala.app",
            "sinet.startup.inDriver"
    };

    private static boolean isTarget(String pkg) {
        for (String t : TARGETS) {
            if (t.equals(pkg)) return true;
        }
        return false;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!isTarget(lpparam.packageName)) return;

        XposedBridge.log("🔥 HTTP/WS HOOK loaded for " + lpparam.packageName);

        /* ===============================
           OkHttpClient.newWebSocket
           =============================== */
        try {
            XposedHelpers.findAndHookMethod(
                    OkHttpClient.class,
                    "newWebSocket",
                    Request.class,
                    WebSocketListener.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            Request req = (Request) param.args[0];

                            XposedBridge.log("🧠 WS CONNECT → " + req.url());
                            XposedBridge.log("🧠 WS HEADERS → " + req.headers());
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object ws = param.getResult();
                            XposedBridge.log("✅ WS OBJECT → " + ws);
                        }
                    }
            );

            XposedBridge.log("✅ newWebSocket hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ newWebSocket hook error: " + t);
        }

        /* ===============================
           WebSocket.send(String)
           =============================== */
        try {
            XposedHelpers.findAndHookMethod(
                    WebSocket.class,
                    "send",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[0];
                            XposedBridge.log("📤 WS SEND (text) → " + msg);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ WS send(String) hook error: " + t);
        }

        /* ===============================
           WebSocket.send(ByteString)
           =============================== */
        try {
            XposedHelpers.findAndHookMethod(
                    WebSocket.class,
                    "send",
                    ByteString.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ByteString bs = (ByteString) param.args[0];
                            XposedBridge.log("📤 WS SEND (bin) → " + bs.hex());
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ WS send(ByteString) hook error: " + t);
        }

        /* ===============================
           WebSocketListener.onMessage(String)
           =============================== */
        try {
            XposedHelpers.findAndHookMethod(
                    WebSocketListener.class,
                    "onMessage",
                    WebSocket.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[1];
                            XposedBridge.log("📥 WS RECV (text) → " + msg);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ WS onMessage(String) hook error: " + t);
        }

        /* ===============================
           WebSocketListener.onMessage(ByteString)
           =============================== */
        try {
            XposedHelpers.findAndHookMethod(
                    WebSocketListener.class,
                    "onMessage",
                    WebSocket.class,
                    ByteString.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ByteString bs = (ByteString) param.args[1];
                            XposedBridge.log("📥 WS RECV (bin) → " + bs.hex());
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ WS onMessage(ByteString) hook error: " + t);
        }
    }
}
