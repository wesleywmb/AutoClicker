package com.autoclicker.config;

import com.autoclicker.listener.HotkeySupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class ConfigRepository {
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_BUTTON = "button";
    private static final String KEY_CLICKTYPE = "clickType";
    private static final String KEY_REPEAT = "repeat";
    private static final String KEY_INFINITE = "infinite";
    private static final String KEY_HOTKEY = "hotkey";
    private static final String KEY_HOTKEY_CODE = "hotkeyCode";
    private static final String KEY_DELAY = "delay";
    private static final String KEY_USE_FIXED = "useFixed";
    private static final String KEY_FIX_X = "fixX";
    private static final String KEY_FIX_Y = "fixY";

    private final Preferences root;

    public ConfigRepository() {
        this(Preferences.userRoot().node("com/autoclicker/profiles"));
    }

    public ConfigRepository(Preferences root) {
        this.root = root;
    }

    public void save(String name, AutoClickerConfig config) {
        String validName = validateName(name);
        config.validate();
        Preferences node = root.node(validName);
        node.putDouble(KEY_INTERVAL, config.getClickIntervalSeconds());
        node.put(KEY_BUTTON, config.getMouseButton().name());
        node.put(KEY_CLICKTYPE, config.getClickType().name());
        node.putInt(KEY_REPEAT, config.getRepeatCount());
        node.putBoolean(KEY_INFINITE, config.isInfiniteRepeat());
        node.put(KEY_HOTKEY, config.getHotkeyActivation());
        node.putInt(KEY_HOTKEY_CODE, config.getHotkeyCode());
        node.putInt(KEY_DELAY, config.getStartDelaySeconds());
        node.putBoolean(KEY_USE_FIXED, config.isUseFixedPosition());
        node.putInt(KEY_FIX_X, config.getFixX());
        node.putInt(KEY_FIX_Y, config.getFixY());
        try {
            node.flush();
        } catch (BackingStoreException | RuntimeException e) {
            throw new ConfigRepositoryException("Não foi possível salvar o perfil.", e);
        }
    }

    public Optional<LoadedProfile> load(String name) {
        String validName = validateName(name);
        try {
            if (!root.nodeExists(validName)) return Optional.empty();
            Preferences node = root.node(validName);
            AutoClickerConfig loaded = new AutoClickerConfig();
            loaded.setClickIntervalSeconds(node.getDouble(KEY_INTERVAL, 0.2));
            loaded.setMouseButton(parseEnum(AutoClickerConfig.MouseButton.class, node.get(KEY_BUTTON, "LEFT")));
            loaded.setClickType(parseEnum(AutoClickerConfig.ClickType.class, node.get(KEY_CLICKTYPE, "SINGLE")));
            loaded.setRepeatCount(node.getInt(KEY_REPEAT, 100));
            loaded.setInfiniteRepeat(node.getBoolean(KEY_INFINITE, true));
            loaded.setStartDelaySeconds(node.getInt(KEY_DELAY, 0));
            loaded.setUseFixedPosition(node.getBoolean(KEY_USE_FIXED, false));
            loaded.setFixX(node.getInt(KEY_FIX_X, 0));
            loaded.setFixY(node.getInt(KEY_FIX_Y, 0));

            String legacyName = node.get(KEY_HOTKEY, "F6");
            int code = node.getInt(KEY_HOTKEY_CODE, -1);
            boolean reset = false;
            if (!HotkeySupport.isKnownCode(code)) code = HotkeySupport.findCode(legacyName);
            if (!HotkeySupport.isKnownCode(code)) {
                code = AutoClickerConfig.DEFAULT_HOTKEY_CODE;
                legacyName = "F6";
                reset = true;
            }
            loaded.setHotkey(code, HotkeySupport.displayName(code));
            loaded.validate();
            return Optional.of(new LoadedProfile(loaded, reset));
        } catch (BackingStoreException | RuntimeException e) {
            throw new ConfigRepositoryException("O perfil contém dados inválidos ou não pôde ser lido.", e);
        }
    }

    public boolean exists(String name) {
        try {
            return root.nodeExists(validateName(name));
        } catch (BackingStoreException | RuntimeException e) {
            throw new ConfigRepositoryException("Não foi possível consultar o perfil.", e);
        }
    }

    public boolean delete(String name) {
        String validName = validateName(name);
        try {
            if (!root.nodeExists(validName)) return false;
            root.node(validName).removeNode();
            root.flush();
            return true;
        } catch (BackingStoreException | RuntimeException e) {
            throw new ConfigRepositoryException("Não foi possível excluir o perfil.", e);
        }
    }

    public List<String> listNames() {
        try {
            List<String> names = new ArrayList<>(List.of(root.childrenNames()));
            names.sort(String.CASE_INSENSITIVE_ORDER);
            return names;
        } catch (BackingStoreException | RuntimeException e) {
            throw new ConfigRepositoryException("Não foi possível listar os perfis.", e);
        }
    }

    public static String validateName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Digite um nome para o perfil.");
        String trimmed = name.trim();
        if (trimmed.length() > Preferences.MAX_NAME_LENGTH || trimmed.equals(".") || trimmed.equals("..")
                || trimmed.indexOf('/') >= 0 || trimmed.indexOf('\\') >= 0 || trimmed.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("O nome do perfil contém caracteres inválidos.");
        }
        return trimmed;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
        return Enum.valueOf(type, value);
    }

    public static final class LoadedProfile {
        private final AutoClickerConfig config;
        private final boolean hotkeyReset;
        LoadedProfile(AutoClickerConfig config, boolean hotkeyReset) { this.config = config; this.hotkeyReset = hotkeyReset; }
        public AutoClickerConfig getConfig() { return config; }
        public boolean isHotkeyReset() { return hotkeyReset; }
    }

    public static final class ConfigRepositoryException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public ConfigRepositoryException(String message, Throwable cause) { super(message, cause); }
    }
}
