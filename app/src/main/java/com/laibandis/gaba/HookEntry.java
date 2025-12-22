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
            Class<?> wsListener =
                    XposedHelpers.findClass(
                            "okhttp3.WebSocketListener",
                            lpparam.classLoader
                    );

            // onMessage(WebSocket, String)
            XposedHelpers.findAndHookMethod(
                    wsListener,
                    "onMessage",
                    Object.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String text = (String) param.args[1];
                            XposedBridge.log("💬 WS TEXT → " + text);
                        }
                    }
            );

            // onMessage(WebSocket, ByteString)
            XposedHelpers.findAndHookMethod(
                    wsListener,
                    "onMessage",
                    Object.class,
                    Object.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Object byteString = param.args[1];
                            byte[] data = (byte[]) XposedHelpers.callMethod(byteString, "toByteArray");
                            String hex = bytesToHex(data);
                            XposedBridge.log("📦 WS BINARY HEX → " + hex);
                        }
                    }
            );

            XposedBridge.log("✅ WS-HOOK WebSocketListener active");

        } catch (Throwable t) {
            XposedBridge.log("❌ WS-HOOK error: " + t);
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
