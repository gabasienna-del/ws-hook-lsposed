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

        XposedBridge.log("🔥 WS-HOOK (headers-all) loaded for " + TARGET);

        try {
            Class<?> builderCls =
                    lpparam.classLoader.loadClass("okhttp3.Request$Builder");

            // addHeader(key, value)
            XposedBridge.hookAllMethods(builderCls, "addHeader", headerHook("addHeader"));

            // header(key, value)
            XposedBridge.hookAllMethods(builderCls, "header", headerHook("header"));

            // headers(Headers)
            XposedBridge.hookAllMethods(
                    builderCls,
                    "headers",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object headers = param.args[0];
                                Map<?, ?> map = (Map<?, ?>) XposedHelpers.callMethod(headers, "toMultimap");

                                for (Map.Entry<?, ?> e : map.entrySet()) {
                                    String key = String.valueOf(e.getKey());
                                    List<?> values = (List<?>) e.getValue();
                                    for (Object v : values) {
                                        XposedBridge.log("📡 headers() → " + key + " = " + v);
                                    }
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("❌ headers() error: " + t);
                            }
                        }
                    }
            );

            // build() — финальный Request
            XposedBridge.hookAllMethods(
                    builderCls,
                    "build",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object request = param.getResult();
                                Object url = XposedHelpers.callMethod(request, "url");
                                Object headers = XposedHelpers.callMethod(request, "headers");

                                XposedBridge.log("🧠 REQUEST BUILT → " + url);

                                Map<?, ?> map = (Map<?, ?>) XposedHelpers.callMethod(headers, "toMultimap");
                                for (Map.Entry<?, ?> e : map.entrySet()) {
                                    String key = String.valueOf(e.getKey());
                                    List<?> values = (List<?>) e.getValue();
                                    for (Object v : values) {
                                        XposedBridge.log("📡 FINAL HEADER → " + key + " = " + v);
                                    }
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("❌ build() error: " + t);
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ Failed to hook Request.Builder: " + t);
        }
    }

    private XC_MethodHook headerHook(String tag) {
        return new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                try {
                    String key = String.valueOf(param.args[0]);
                    String value = String.valueOf(param.args[1]);

                    XposedBridge.log("📡 " + tag + " → " + key + " = " + value);
                } catch (Throwable t) {
                    XposedBridge.log("❌ " + tag + " error: " + t);
                }
            }
        };
    }
}
