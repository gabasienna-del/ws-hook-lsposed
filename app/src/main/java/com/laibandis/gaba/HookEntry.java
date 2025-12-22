package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.util.List;
import java.util.Map;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {

        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + TARGET);

        try {
            Class<?> okHttpClientCls =
                    lpparam.classLoader.loadClass("okhttp3.OkHttpClient");

            XposedBridge.hookAllMethods(
                    okHttpClientCls,
                    "newWebSocket",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object request = param.args[0];

                                // URL
                                Object url = XposedHelpers.callMethod(request, "url");
                                XposedBridge.log("🧠 WS CONNECT → " + url);

                                // Headers
                                Object headers = XposedHelpers.callMethod(request, "headers");
                                Map<?, ?> map = (Map<?, ?>) XposedHelpers.callMethod(headers, "toMultimap");

                                for (Map.Entry<?, ?> e : map.entrySet()) {
                                    String key = String.valueOf(e.getKey());
                                    List<?> values = (List<?>) e.getValue();

                                    for (Object v : values) {
                                        XposedBridge.log("📡 WS HEADER → " + key + " = " + v);
                                    }
                                }

                            } catch (Throwable t) {
                                XposedBridge.log("❌ WS HEADER error: " + t);
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ Failed to hook OkHttp WS: " + t);
        }
    }
}
