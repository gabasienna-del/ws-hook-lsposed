package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("🔥 HTTP-HOOK loaded for " + lpparam.packageName);

        // ==============================
        // STEP 1 — HTTP HOOK (LOGIN / TOKEN)
        // ==============================

        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.RealCall",
                    lpparam.classLoader,
                    "execute",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object call = param.thisObject;
                                Object request = XposedHelpers.callMethod(call, "request");

                                String method = (String) XposedHelpers.callMethod(request, "method");
                                Object url = XposedHelpers.callMethod(request, "url");

                                XposedBridge.log("🌐 HTTP EXECUTE → " + method + " " + url);

                                Object headers = XposedHelpers.callMethod(request, "headers");
                                XposedBridge.log("📩 HEADERS → " + headers.toString());

                                Object body = XposedHelpers.callMethod(request, "body");
                                if (body != null) {
                                    XposedBridge.log("📦 BODY CLASS → " + body.getClass().getName());
                                }
                            } catch (Throwable t) {
                                XposedBridge.log("❌ HTTP execute inner error: " + t);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ HTTP execute hook failed: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.RealCall",
                    lpparam.classLoader,
                    "enqueue",
                    "okhttp3.Callback",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object call = param.thisObject;
                                Object request = XposedHelpers.callMethod(call, "request");

                                String method = (String) XposedHelpers.callMethod(request, "method");
                                Object url = XposedHelpers.callMethod(request, "url");

                                XposedBridge.log("🌐 HTTP ENQUEUE → " + method + " " + url);
                            } catch (Throwable t) {
                                XposedBridge.log("❌ HTTP enqueue inner error: " + t);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ HTTP enqueue hook failed: " + t);
        }
    }
}
