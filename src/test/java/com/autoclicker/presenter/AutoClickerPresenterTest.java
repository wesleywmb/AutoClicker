package com.autoclicker.presenter;

import com.autoclicker.config.AutoClickerConfig;
import com.autoclicker.engine.AutoClickerEngine;
import com.autoclicker.engine.MouseController;
import com.autoclicker.engine.SessionMetrics;
import com.autoclicker.macro.MacroEngine;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class AutoClickerPresenterTest {
    @Test void stopIsAsynchronousAndStateReturnsToStopped() throws Exception {
        AutoClickerConfig config = new AutoClickerConfig();
        MouseController mouse = new NoOpMouse();
        AutoClickerEngine clickEngine = new AutoClickerEngine(config, mouse, System::nanoTime, Thread::sleep);
        MacroEngine macroEngine = new MacroEngine(config, mouse, System::nanoTime, Thread::sleep);
        RecordingView view = new RecordingView();
        AutoClickerPresenter presenter = new AutoClickerPresenter(config, clickEngine, macroEngine, view);

        SwingUtilities.invokeAndWait(() -> assertTrue(presenter.startClicking()));
        AtomicReference<ExecutionState> immediateState = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            presenter.stopClicking();
            immediateState.set(presenter.getState());
        });
        assertEquals(ExecutionState.STOPPING, immediateState.get());
        assertTrue(view.stopped.await(2, TimeUnit.SECONDS));
        assertEquals(ExecutionState.STOPPED, presenter.getState());
        presenter.shutdown();
    }

    @Test void controllerToggleUsesTheSameStartValidatorAsTheButton() throws Exception {
        AutoClickerConfig config = new AutoClickerConfig();
        MouseController mouse = new NoOpMouse();
        AutoClickerEngine clickEngine = new AutoClickerEngine(config, mouse, System::nanoTime, Thread::sleep);
        MacroEngine macroEngine = new MacroEngine(config, mouse, System::nanoTime, Thread::sleep);
        AutoClickerPresenter presenter = new AutoClickerPresenter(config, clickEngine, macroEngine, new RecordingView());
        presenter.setStartValidator(() -> false);

        SwingUtilities.invokeAndWait(presenter::toggleClicking);
        assertEquals(ExecutionState.STOPPED, presenter.getState());
        assertFalse(clickEngine.isActive());
        presenter.shutdown();
    }

    private static final class NoOpMouse implements MouseController {
        public void move(int x, int y) { }
        public void press(int mask) { }
        public void release(int mask) { }
    }

    private static final class RecordingView implements IAutoClickerView {
        private final CountDownLatch stopped = new CountDownLatch(1);
        private volatile boolean wasActive;
        public void onExecutionStateChanged(ExecutionState state, boolean macroMode) {
            if (state == ExecutionState.RUNNING || state == ExecutionState.COUNTDOWN || state == ExecutionState.STOPPING) wasActive = true;
            if (wasActive && state == ExecutionState.STOPPED) stopped.countDown();
        }
        public void onStatusTick(SessionMetrics metrics, long total, boolean infinite, boolean macroMode) { }
        public void onCountdown(int secondsLeft) { }
        public void onActionIndexChanged(int index) { }
        public void onExecutionFinished(com.autoclicker.engine.ExecutionEvent.StopReason reason) { }
        public void onExecutionError(String message, Throwable error) { fail(message); }
    }
}
