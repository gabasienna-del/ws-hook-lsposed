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

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + TARGET);

        // WebSocket connect
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
                        XposedBridge.log("🧠 WS CONNECT → " + req.url());
                        XposedBridge.log("🧠 HEADERS → " + req.headers());
                    }
                }
        );

        // Incoming WebSocket messages
        XposedHelpers.findAndHookMethod(
                "okhttp3.WebSocketListener",
                lpparam.classLoader,
                "onMessage",
                WebSocket.class,
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        String msg = (String) param.args[1];
                        XposedBridge.log("🔥 WS MESSAGE → " + msg);
                    }
                }
        );
    }
}
