package com.laibandis.gaba;

import java.nio.charset.StandardCharsets;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okio.Buffer;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "🔥 HTTP-HOOK";

    // Целевые приложения
    private static final String[] TARGETS = new String[] {
            "kz.asemainala.app",
            "sinet.startup.inDriver"
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        boolean match = false;
        for (String p : TARGETS) {
            if (p.equals(lpparam.packageName)) {
                match = true;
                break;
            }
        }
        if (!match) return;

        XposedBridge.log(TAG + " loaded for " + lpparam.packageName);

        hookRequestBody(lpparam);
        hookResponseBody(lpparam);
    }

    /* ===============================
       HTTP REQUEST BODY
       =============================== */
    private void hookRequestBody(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.RequestBody",
                    lpparam.classLoader,
                    "writeTo",
                    "okio.BufferedSink",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Buffer buffer = new Buffer();
                                Object body = param.thisObject;

                                XposedHelpers.callMethod(body, "writeTo", buffer);

                                String data = buffer.readString(StandardCharsets.UTF_8);
                                if (data != null && data.length() > 0 && data.length() < 20000) {
                                    XposedBridge.log(TAG + " ▶ REQUEST BODY:\n" + data);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " body error: " + t);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + " hook RequestBody failed: " + t);
        }
    }

    /* ===============================
       HTTP RESPONSE BODY
       =============================== */
    private void hookResponseBody(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.ResponseBody",
                    lpparam.classLoader,
                    "string",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                String resp = (String) param.getResult();
                                if (resp != null && resp.length() > 0 && resp.length() < 50000) {
                                    XposedBridge.log(TAG + " ◀ RESPONSE:\n" + resp);
                                }
                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " response error: " + t);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + " hook ResponseBody failed: " + t);
        }
    }
}
