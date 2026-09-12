package com.autoclicker.listener;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class HotkeySupport {
    private HotkeySupport() {}

    public static String displayName(int code) {
        String name = NativeKeyEvent.getKeyText(code);
        return name == null || name.isBlank() ? "F6" : name;
    }

    public static int findCode(String legacyName) {
        if (legacyName == null || legacyName.isBlank()) return -1;
        String wanted = normalize(legacyName);
        for (Field field : NativeKeyEvent.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class || !field.getName().startsWith("VC_")) continue;
            try {
                int code = field.getInt(null);
                if (normalize(displayName(code)).equals(wanted)
                        || normalize(field.getName().substring(3)).equals(wanted)) return code;
            } catch (IllegalAccessException ignored) {
                // Campos VC_* são públicos; apenas ignora uma implementação incomum.
            }
        }
        return -1;
    }

    public static boolean isKnownCode(int wantedCode) {
        for (Field field : NativeKeyEvent.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != int.class || !field.getName().startsWith("VC_")) continue;
            try {
                if (field.getInt(null) == wantedCode && !field.getName().equals("VC_UNDEFINED")) return true;
            } catch (IllegalAccessException ignored) { }
        }
        return false;
    }

    private static String normalize(String value) {
        return value.trim().replace(" ", "").replace("_", "").toUpperCase();
    }
}
