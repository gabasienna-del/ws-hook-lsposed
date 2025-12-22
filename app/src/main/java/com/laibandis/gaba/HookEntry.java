package com.laibandis.gaba;

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

        try {
            // Загружаем OkHttpClient ИМЕННО из ClassLoader приложения
            Class<?> okHttpClientCls =
                    lpparam.classLoader.loadClass("okhttp3.OkHttpClient");

            // Хукаем ВСЕ newWebSocket(...)
            XposedBridge.hookAllMethods(
                    okHttpClientCls,
                    "newWebSocket",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object request = param.args[0];
                                Object url = XposedHelpers.callMethod(request, "url");

                                XposedBridge.log("🧠 WS CONNECT → " + url.toString());
                            } catch (Throwable t) {
                                XposedBridge.log("WS CONNECT error: " + t);
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ Failed to hook OkHttp WS: " + t);
        }
    }
}
