package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import okhttp3.WebSocket;
import okio.ByteString;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + TARGET);

        try {
            XposedHelpers.findAndHookMethod(
                    WebSocket.class,
                    "onMessage",
                    ByteString.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ByteString bs = (ByteString) param.args[0];
                            byte[] data = bs.toByteArray();

                            String hex = bytesToHex(data);
                            XposedBridge.log("📦 WS BINARY HEX → " + hex);
                        }
                    }
            );

            XposedBridge.log("✅ WS-HOOK onMessage(ByteString) active");

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
