package com.autoclicker.listener;

import com.autoclicker.presenter.IClickController;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import javax.swing.SwingUtilities;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HotkeyListener implements NativeKeyListener {
    private static final Logger LOG = Logger.getLogger(HotkeyListener.class.getName());
    private final IClickController controller;
    private final Set<Integer> pressedCodes = ConcurrentHashMap.newKeySet();
    private volatile BiConsumer<Integer, String> captureCallback;

    public HotkeyListener(IClickController controller) { this.controller = controller; }

    public void register() {
        silenceLogs();
        try {
            if (!GlobalScreen.isNativeHookRegistered()) GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
        } catch (NativeHookException e) {
            throw new IllegalStateException("Não foi possível registrar o atalho global.", e);
        }
    }

    public void unregister() {
        captureCallback = null;
        pressedCodes.clear();
        try {
            GlobalScreen.removeNativeKeyListener(this);
            if (GlobalScreen.isNativeHookRegistered()) GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            LOG.log(Level.FINE, "Falha ao remover o atalho global", e);
        }
    }

    public void beginCapture(BiConsumer<Integer, String> callback) { captureCallback = callback; }
    public void cancelCapture() { captureCallback = null; }

    @Override public void nativeKeyPressed(NativeKeyEvent event) {
        int code = event.getKeyCode();
        if (!pressedCodes.add(code)) return;
        BiConsumer<Integer, String> capture = captureCallback;
        if (capture != null) {
            captureCallback = null;
            SwingUtilities.invokeLater(() -> capture.accept(code, HotkeySupport.displayName(code)));
        } else if (code == controller.getHotkeyCode() && controller.isActivationAllowed()) {
            SwingUtilities.invokeLater(controller::toggleClicking);
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent event) {
        pressedCodes.remove(event.getKeyCode());
    }
    @Override public void nativeKeyTyped(NativeKeyEvent event) {}

    private static void silenceLogs() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);
    }
}
