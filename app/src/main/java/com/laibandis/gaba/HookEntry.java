package com.laibandis.gaba;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {

        if (!"sinet.startup.inDriver".equals(lpparam.packageName)
                && !"kz.asemainala.app".equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("🔥 HTTP/WS HOOK loaded for " + lpparam.packageName);

        try {
            // === Load classes via app ClassLoader ===
            Class<?> OkHttpClient = XposedHelpers.findClass(
                    "okhttp3.OkHttpClient",
                    lpparam.classLoader
            );

            Class<?> Request = XposedHelpers.findClass(
                    "okhttp3.Request",
                    lpparam.classLoader
            );

            Class<?> WebSocketListener = XposedHelpers.findClass(
                    "okhttp3.WebSocketListener",
                    lpparam.classLoader
            );

            // === Hook newWebSocket ===
            XposedHelpers.findAndHookMethod(
                    OkHttpClient,
                    "newWebSocket",
                    Request,
                    WebSocketListener,
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object request = param.args[0];
                                Object url = XposedHelpers.callMethod(request, "url");

                                XposedBridge.log("🧠 WS CONNECT → " + url);

                                Object headers = XposedHelpers.callMethod(request, "headers");
                                XposedBridge.log("🧠 WS HEADERS → " + headers);

                            } catch (Throwable t) {
                                XposedBridge.log("❌ WS beforeHook error: "
                                        + Log.getStackTraceString(t));
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object ws = param.getResult();
                                XposedBridge.log("✅ WS OBJECT → " + ws);
                            } catch (Throwable t) {
                                XposedBridge.log("❌ WS afterHook error: "
                                        + Log.getStackTraceString(t));
                            }
                        }
                    }
            );

            XposedBridge.log("✅ newWebSocket hook OK");

        } catch (Throwable t) {
            XposedBridge.log("❌ WS hook failed: "
                    + Log.getStackTraceString(t));
        }
    }
}
