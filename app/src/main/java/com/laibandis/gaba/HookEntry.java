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

        XposedBridge.log("🔥 WS-HOOK (addHeader) loaded for " + TARGET);

        try {
            Class<?> builderCls =
                    lpparam.classLoader.loadClass("okhttp3.Request$Builder");

            XposedBridge.hookAllMethods(
                    builderCls,
                    "addHeader",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {

                            try {
                                String key = String.valueOf(param.args[0]);
                                String value = String.valueOf(param.args[1]);

                                // фильтр, чтобы не заспамить лог
                                if (key.equalsIgnoreCase("Authorization")
                                        || key.toLowerCase().contains("token")
                                        || key.toLowerCase().contains("cookie")
                                        || key.toLowerCase().contains("device")) {

                                    XposedBridge.log(
                                            "📡 addHeader → " + key + " = " + value
                                    );
                                }

                            } catch (Throwable t) {
                                XposedBridge.log("❌ addHeader error: " + t);
                            }
                        }
                    }
            );

        } catch (Throwable t) {
            XposedBridge.log("❌ Failed to hook addHeader: " + t);
        }
    }
}
