package com.autoclicker.presenter;

import com.autoclicker.config.AutoClickerConfig;
import com.autoclicker.engine.AutoClickerEngine;
import com.autoclicker.engine.ExecutionEvent;
import com.autoclicker.engine.SessionMetrics;
import com.autoclicker.macro.MacroEngine;
import com.autoclicker.macro.MacroSequence;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.BooleanSupplier;

public final class AutoClickerPresenter implements IClickController {
    private final AutoClickerConfig config;
    private final AutoClickerEngine clickEngine;
    private final MacroEngine macroEngine;
    private final IAutoClickerView view;
    private final MacroSequence macroSequence = new MacroSequence();
    private final AtomicLong sessionSequence = new AtomicLong();
    private final AtomicInteger latestStep = new AtomicInteger(-1);
    private final Timer statusTimer;
    private final Consumer<ExecutionEvent> clickListener = this::receiveEvent;
    private final Consumer<ExecutionEvent> macroListener = this::receiveEvent;

    private volatile ExecutionState state = ExecutionState.STOPPED;
    private volatile long activeSessionId;
    private volatile boolean macroMode;
    private volatile boolean activeMacroMode;
    private volatile boolean activationSuspended;
    private long activeTotal;
    private boolean activeInfinite;
    private int renderedStep = -1;
    private BooleanSupplier startValidator = () -> true;

    public AutoClickerPresenter(AutoClickerConfig config, AutoClickerEngine clickEngine, IAutoClickerView view) {
        this(config, clickEngine, new MacroEngine(config), view);
    }

    public AutoClickerPresenter(AutoClickerConfig config, AutoClickerEngine clickEngine,
                                MacroEngine macroEngine, IAutoClickerView view) {
        this.config = config;
        this.clickEngine = clickEngine;
        this.macroEngine = macroEngine;
        this.view = view;
        clickEngine.addListener(clickListener);
        macroEngine.addListener(macroListener);
        statusTimer = new Timer(200, event -> publishStatus());
        statusTimer.setCoalesce(true);
    }

    public boolean startClicking() {
        if (state != ExecutionState.STOPPED || activationSuspended) return false;
        if (!startValidator.getAsBoolean()) return false;
        if (macroMode && macroSequence.isEmpty()) {
            view.onExecutionError("A sequência de macro está vazia.", null);
            return false;
        }

        long id = sessionSequence.incrementAndGet();
        activeSessionId = id;
        activeMacroMode = macroMode;
        activeTotal = config.getRepeatCount();
        activeInfinite = config.isInfiniteRepeat();
        latestStep.set(-1);
        renderedStep = -1;
        setState(config.getStartDelaySeconds() > 0 ? ExecutionState.COUNTDOWN : ExecutionState.RUNNING);
        boolean started;
        try {
            started = activeMacroMode ? macroEngine.start(id, macroSequence) : clickEngine.start(id);
        } catch (RuntimeException e) {
            setState(ExecutionState.STOPPED);
            view.onExecutionError("Não foi possível iniciar a execução.", e);
            return false;
        }
        if (!started) {
            setState(ExecutionState.STOPPED);
            return false;
        }
        statusTimer.start();
        return true;
    }

    public void stopClicking() {
        if (state == ExecutionState.STOPPED || state == ExecutionState.STOPPING) return;
        setState(ExecutionState.STOPPING);
        if (activeMacroMode) macroEngine.stop(); else clickEngine.stop();
    }

    @Override public void toggleClicking() {
        if (state == ExecutionState.STOPPED) startClicking();
        else if (state == ExecutionState.COUNTDOWN || state == ExecutionState.RUNNING) stopClicking();
    }

    private void receiveEvent(ExecutionEvent event) {
        if (event.getType() == ExecutionEvent.Type.STEP) {
            if (event.getSessionId() == activeSessionId) latestStep.set(event.getValue());
            return;
        }
        if (SwingUtilities.isEventDispatchThread()) handleEvent(event);
        else SwingUtilities.invokeLater(() -> handleEvent(event));
    }

    private void handleEvent(ExecutionEvent event) {
        if (event.getSessionId() != activeSessionId) return;
        switch (event.getType()) {
            case COUNTDOWN:
                if (state != ExecutionState.STOPPING) {
                    setState(ExecutionState.COUNTDOWN);
                    view.onCountdown(event.getValue());
                }
                break;
            case STARTED:
                if (state != ExecutionState.STOPPING) setState(ExecutionState.RUNNING);
                break;
            case STEP:
                latestStep.set(event.getValue());
                break;
            case STOPPED:
                publishStatus();
                statusTimer.stop();
                setState(ExecutionState.STOPPED);
                view.onExecutionFinished(event.getStopReason());
                if (event.getStopReason() == ExecutionEvent.StopReason.FAILED) {
                    view.onExecutionError("A execução foi interrompida por uma falha.", event.getError());
                }
                break;
            default:
                break;
        }
    }

    private void publishStatus() {
        SessionMetrics metrics = activeMacroMode ? macroEngine.getMetrics() : clickEngine.getMetrics();
        view.onStatusTick(metrics, activeTotal, activeInfinite, activeMacroMode);
        int step = latestStep.get();
        if (activeMacroMode && step >= 0 && step != renderedStep) {
            renderedStep = step;
            view.onActionIndexChanged(step);
        }
    }

    private void setState(ExecutionState newState) {
        state = newState;
        view.onExecutionStateChanged(newState, activeMacroMode);
    }

    public void shutdown() {
        activationSuspended = true;
        statusTimer.stop();
        if (state != ExecutionState.STOPPED) {
            if (activeMacroMode) macroEngine.stop(); else clickEngine.stop();
        }
        clickEngine.removeListener(clickListener);
        macroEngine.removeListener(macroListener);
    }

    public ExecutionState getState() { return state; }
    public boolean isMacroMode() { return macroMode; }
    public void setMacroMode(boolean enabled) { if (state == ExecutionState.STOPPED) macroMode = enabled; }
    public MacroSequence getMacroSequence() { return macroSequence; }
    public void setActivationSuspended(boolean suspended) { activationSuspended = suspended; }
    public void setStartValidator(BooleanSupplier validator) {
        startValidator = validator == null ? () -> true : validator;
    }
    @Override public int getHotkeyCode() { return config.getHotkeyCode(); }
    @Override public boolean isActivationAllowed() { return !activationSuspended && state != ExecutionState.STOPPING; }
}
