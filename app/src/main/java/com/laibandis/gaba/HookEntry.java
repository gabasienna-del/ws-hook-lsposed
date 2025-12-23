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

    private static final String[] TARGETS = {
            "kz.asemainala.app",
            "sinet.startup.inDriver"
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        boolean ok = false;
        for (String p : TARGETS) {
            if (p.equals(lpparam.packageName)) {
                ok = true;
                break;
            }
        }
        if (!ok) return;

        XposedBridge.log(TAG + " loaded for " + lpparam.packageName);

        hookInterceptor(lpparam);
    }

    private void hookInterceptor(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.Interceptor",
                    lpparam.classLoader,
                    "intercept",
                    "okhttp3.Interceptor$Chain",
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object chain = param.args[0];
                                Object request = XposedHelpers.callMethod(chain, "request");

                                String url = String.valueOf(
                                        XposedHelpers.callMethod(
                                                XposedHelpers.callMethod(request, "url"),
                                                "toString"
                                        )
                                );

                                Object headers = XposedHelpers.callMethod(request, "headers");
                                XposedBridge.log(TAG + " ▶ URL: " + url);
                                XposedBridge.log(TAG + " ▶ HEADERS:\n" + headers);

                                Object body = XposedHelpers.callMethod(request, "body");
                                if (body != null) {
                                    Buffer buffer = new Buffer();
                                    XposedHelpers.callMethod(body, "writeTo", buffer);
                                    String data = buffer.readString(StandardCharsets.UTF_8);
                                    if (!data.isEmpty()) {
                                        XposedBridge.log(TAG + " ▶ BODY:\n" + data);
                                    }
                                }

                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " request parse error: " + t);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object response = param.getResult();
                                Object body = XposedHelpers.callMethod(response, "body");
                                if (body == null) return;

                                Object source = XposedHelpers.callMethod(body, "source");
                                XposedHelpers.callMethod(source, "request", Long.MAX_VALUE);

                                Object buffer = XposedHelpers.callMethod(source, "buffer");
                                String resp = XposedHelpers.callMethod(buffer, "clone")
                                        .toString();

                                if (!resp.isEmpty()) {
                                    XposedBridge.log(TAG + " ◀ RESPONSE:\n" + resp);
                                }

                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " response parse error: " + t);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + " interceptor hook failed: " + t);
        }
    }
}
