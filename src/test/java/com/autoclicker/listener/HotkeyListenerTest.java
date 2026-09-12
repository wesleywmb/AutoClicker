package com.autoclicker.listener;

import com.autoclicker.presenter.IClickController;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class HotkeyListenerTest {
    @Test void heldKeyTogglesOnlyOnceUntilReleased() throws Exception {
        FakeController controller = new FakeController();
        HotkeyListener listener = new HotkeyListener(controller);
        NativeKeyEvent pressed = event(NativeKeyEvent.NATIVE_KEY_PRESSED);
        NativeKeyEvent released = event(NativeKeyEvent.NATIVE_KEY_RELEASED);

        listener.nativeKeyPressed(pressed);
        listener.nativeKeyPressed(pressed);
        flushEdt();
        assertEquals(1, controller.toggles.get());

        listener.nativeKeyReleased(released);
        listener.nativeKeyPressed(pressed);
        flushEdt();
        assertEquals(2, controller.toggles.get());
    }

    @Test void captureDoesNotToggleExecution() throws Exception {
        FakeController controller = new FakeController();
        HotkeyListener listener = new HotkeyListener(controller);
        AtomicInteger captured = new AtomicInteger();
        listener.beginCapture((code, name) -> captured.set(code));
        listener.nativeKeyPressed(event(NativeKeyEvent.NATIVE_KEY_PRESSED));
        flushEdt();
        assertEquals(NativeKeyEvent.VC_F6, captured.get());
        assertEquals(0, controller.toggles.get());
    }

    @Test void anotherKeyDoesNotResetHeldHotkey() throws Exception {
        FakeController controller = new FakeController();
        HotkeyListener listener = new HotkeyListener(controller);

        listener.nativeKeyPressed(event(NativeKeyEvent.NATIVE_KEY_PRESSED, NativeKeyEvent.VC_F6));
        listener.nativeKeyPressed(event(NativeKeyEvent.NATIVE_KEY_PRESSED, NativeKeyEvent.VC_A));
        listener.nativeKeyPressed(event(NativeKeyEvent.NATIVE_KEY_PRESSED, NativeKeyEvent.VC_F6));
        flushEdt();

        assertEquals(1, controller.toggles.get());
        listener.nativeKeyReleased(event(NativeKeyEvent.NATIVE_KEY_RELEASED, NativeKeyEvent.VC_F6));
        listener.nativeKeyPressed(event(NativeKeyEvent.NATIVE_KEY_PRESSED, NativeKeyEvent.VC_F6));
        flushEdt();
        assertEquals(2, controller.toggles.get());
    }

    private static NativeKeyEvent event(int id) {
        return event(id, NativeKeyEvent.VC_F6);
    }

    private static NativeKeyEvent event(int id, int keyCode) {
        return new NativeKeyEvent(id, 0, 0, keyCode,
                NativeKeyEvent.CHAR_UNDEFINED, NativeKeyEvent.KEY_LOCATION_STANDARD);
    }

    private static void flushEdt() throws Exception { SwingUtilities.invokeAndWait(() -> { }); }

    private static final class FakeController implements IClickController {
        private final AtomicInteger toggles = new AtomicInteger();
        public void toggleClicking() { toggles.incrementAndGet(); }
        public int getHotkeyCode() { return NativeKeyEvent.VC_F6; }
        public boolean isActivationAllowed() { return true; }
    }
}
