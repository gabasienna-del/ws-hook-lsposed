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
        boolean match = false;
        for (String p : TARGETS) {
            if (p.equals(lpparam.packageName)) {
                match = true;
                break;
            }
        }
        if (!match) return;

        XposedBridge.log(TAG + " loaded for " + lpparam.packageName);

        hookRealInterceptorChain(lpparam);
    }

    private void hookRealInterceptorChain(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.internal.http.RealInterceptorChain",
                    lpparam.classLoader,
                    "proceed",
                    "okhttp3.Request",
                    new XC_MethodHook() {

                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object request = param.args[0];

                                Object urlObj = XposedHelpers.callMethod(request, "url");
                                String url = String.valueOf(
                                        XposedHelpers.callMethod(urlObj, "toString")
                                );

                                Object headers = XposedHelpers.callMethod(request, "headers");

                                XposedBridge.log(TAG + " ▶ URL: " + url);
                                XposedBridge.log(TAG + " ▶ HEADERS:\n" + headers);

                                Object body = XposedHelpers.callMethod(request, "body");
                                if (body != null) {
                                    Buffer buffer = new Buffer();
                                    XposedHelpers.callMethod(body, "writeTo", buffer);
                                    String bodyStr = buffer.readString(StandardCharsets.UTF_8);
                                    if (!bodyStr.isEmpty()) {
                                        XposedBridge.log(TAG + " ▶ BODY:\n" + bodyStr);
                                    }
                                }

                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " request error: " + t);
                            }
                        }

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Object response = param.getResult();
                                if (response == null) return;

                                Object body = XposedHelpers.callMethod(response, "body");
                                if (body == null) return;

                                Object source = XposedHelpers.callMethod(body, "source");
                                XposedHelpers.callMethod(source, "request", Long.MAX_VALUE);

                                Object buffer = XposedHelpers.callMethod(source, "buffer");
                                String resp = XposedHelpers.callMethod(buffer, "clone").toString();

                                if (!resp.isEmpty()) {
                                    XposedBridge.log(TAG + " ◀ RESPONSE:\n" + resp);
                                }

                            } catch (Throwable t) {
                                XposedBridge.log(TAG + " response error: " + t);
                            }
                        }
                    }
            );

            XposedBridge.log(TAG + " RealInterceptorChain hooked OK");

        } catch (Throwable t) {
            XposedBridge.log(TAG + " hook failed: " + t);
        }
    }
}
