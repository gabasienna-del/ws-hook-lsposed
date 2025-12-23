package com.laibandis.gaba;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import java.nio.ByteBuffer;

public class HookEntry implements IXposedHookLoadPackage {

    private static final String TARGET = "kz.asemainala.app";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET)) return;

        XposedBridge.log("🔥 WS-HOOK loaded for " + TARGET);

        // ========== TEXT WS ==========
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.RealWebSocket",
                    lpparam.classLoader,
                    "send",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String msg = (String) param.args[0];
                            XposedBridge.log("🟢 WS TEXT → " + msg);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ TEXT hook failed: " + t);
        }

        // ========== BINARY WS ==========
        try {
            XposedHelpers.findAndHookMethod(
                    "okhttp3.RealWebSocket",
                    lpparam.classLoader,
                    "send",
                    ByteBuffer.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            ByteBuffer buf = (ByteBuffer) param.args[0];
                            if (buf == null) return;

                            byte[] data = new byte[buf.remaining()];
                            buf.get(data);

                            XposedBridge.log("🔵 WS BINARY (" + data.length + " bytes)");
                            XposedBridge.log("HEX → " + bytesToHex(data));

                            buf.rewind();
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log("❌ BINARY hook failed: " + t);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
