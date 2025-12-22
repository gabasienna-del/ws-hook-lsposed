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

        XposedBridge.log("🔥 WS-HOOK (Interceptor) loaded for " + TARGET);

        try {
            Class<?> interceptorCls =
                    lpparam.classLoader.loadClass("okhttp3.Interceptor");

            XposedBridge.hookAllMethods(
                    interceptorCls,
                    "intercept",
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object chain = param.args[0];

                                // Request
                                Object request = XposedHelpers.callMethod(chain, "request");

                                // URL
                                Object url = XposedHelpers.callMethod(request, "url");
                                String urlStr = String.valueOf(url);

                                // Фильтр — только WS порт
                                if (!urlStr.contains(":20413")) return;

                                XposedBridge.log("🧠 INTERCEPT → " + urlStr);

                                // Headers
                                Object headers = XposedHelpers.callMethod(request, "headers");
                                Map<?, ?> map = (Map<?, ?>) XposedHelpers.callMethod(headers, "toMultimap");

                                for (Map.Entry<?, ?> e : map.entrySet()) {
                                    String key = String.valueOf(e.getKey());
                                    List<?> values = (List<?>) e.getValue();
                                    for (Object v : values) {
                                        XposedBridge.log("📡 HEADER → " + key + " = " + v);
                                    }
                                }

                            } catch (Throwable t) {
                                XposedBridge.log("❌ INTERCEPT error: " + t);
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ Failed to hook Interceptor: " + t);
        }
    }
}
