package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET.equals(lpparam.packageName)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + TARGET);

        try {
            Class<?> wsListener = XposedHelpers.findClass(
                    "okhttp3.WebSocketListener",
                    lpparam.classLoader
            );

            XposedHelpers.hookAllMethods(
                    wsListener,
                    "onMessage",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                            if (param.args.length < 2) return;

                            Object payload = param.args[1];

                            // TEXT
                            if (payload instanceof String) {
                                XposedBridge.log("💬 WS TEXT → " + payload);
                                return;
                            }

                            // BINARY (ByteString)
                            try {
                                byte[] data = (byte[]) XposedHelpers.callMethod(
                                        payload,
                                        "toByteArray"
                                );
                                String hex = bytesToHex(data);
                                XposedBridge.log("📦 WS BINARY HEX → " + hex);
                            } catch (Throwable ignore) {
                                // not ByteString
                            }
                        }
                    }
            );

            XposedBridge.log("✅ WS-HOOK WebSocketListener.onMessage hooked");

        } catch (Throwable t) {
            XposedBridge.log("❌ WS-HOOK fatal error: " + t);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
