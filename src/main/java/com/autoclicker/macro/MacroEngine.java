package com.autoclicker.macro;

import com.autoclicker.config.AutoClickerConfig;
import com.autoclicker.engine.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class MacroEngine {
    private final AutoClickerConfig config;
    private final MouseController mouse;
    private final MonotonicClock clock;
    private final Sleeper sleeper;
    private final CopyOnWriteArrayList<Consumer<ExecutionEvent>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean stopRequested = new AtomicBoolean();
    private final AtomicLong completedCycles = new AtomicLong();
    private volatile Thread worker;
    private volatile long startNanos;
    private volatile long frozenElapsedNanos;

    public MacroEngine(AutoClickerConfig config) {
        this(config, new RobotMouseController(), MonotonicClock.system(), Sleeper.threadSleeper());
    }

    public MacroEngine(AutoClickerConfig config, MouseController mouse, MonotonicClock clock, Sleeper sleeper) {
        this.config = config;
        this.mouse = mouse;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public synchronized boolean start(long sessionId, MacroSequence sequence) {
        if ((worker != null && worker.isAlive()) || sequence.isEmpty()) return false;
        AutoClickerConfig snapshot = config.copy();
        List<ClickAction> actions = sequence.snapshot();
        stopRequested.set(false);
        completedCycles.set(0);
        startNanos = 0;
        frozenElapsedNanos = 0;
        worker = new Thread(() -> runSession(sessionId, snapshot, actions), "MacroEngine-" + sessionId);
        worker.setDaemon(true);
        worker.start();
        return true;
    }

    public void stop() {
        stopRequested.set(true);
        Thread current = worker;
        if (current != null) current.interrupt();
    }

    private void runSession(long id, AutoClickerConfig snapshot, List<ClickAction> actions) {
        ExecutionEvent.StopReason reason = ExecutionEvent.StopReason.CANCELLED;
        Throwable failure = null;
        try {
            for (int remaining = snapshot.getStartDelaySeconds(); remaining > 0; remaining--) {
                checkCancelled();
                fire(ExecutionEvent.countdown(id, remaining));
                sleeper.sleep(1000);
            }
            checkCancelled();
            startNanos = clock.nanoTime();
            fire(ExecutionEvent.started(id));
            while (true) {
                checkCancelled();
                for (int step = 0; step < actions.size(); step++) {
                    checkCancelled();
                    fire(ExecutionEvent.step(id, step));
                    execute(actions.get(step));
                }
                long cycles = completedCycles.incrementAndGet();
                if (!snapshot.isInfiniteRepeat() && cycles >= snapshot.getRepeatCount()) {
                    reason = ExecutionEvent.StopReason.COMPLETED;
                    break;
                }
                if (snapshot.isInfiniteRepeat()) sleeper.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            failure = e;
            reason = ExecutionEvent.StopReason.FAILED;
        } finally {
            freezeElapsed();
            synchronized (this) { if (Thread.currentThread() == worker) worker = null; }
            fire(ExecutionEvent.stopped(id, reason, failure));
        }
    }

    private void execute(ClickAction action) throws InterruptedException {
        switch (action.getActionType()) {
            case CLICK:
                mouse.move(action.getX(), action.getY());
                for (int i = 0; i < action.getClickType().getClickCount(); i++) {
                    checkCancelled();
                    int mask = action.getMouseButton().getMask();
                    try { mouse.press(mask); }
                    finally { mouse.release(mask); }
                    if (i + 1 < action.getClickType().getClickCount()) sleeper.sleep(50);
                }
                break;
            case WAIT:
                if (action.getWaitMs() > 0) sleeper.sleep(action.getWaitMs());
                break;
            case MOVE:
                mouse.move(action.getX(), action.getY());
                break;
            default:
                throw new IllegalStateException("Ação de macro desconhecida.");
        }
    }

    private void checkCancelled() throws InterruptedException {
        if (stopRequested.get() || Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }

    private void freezeElapsed() { frozenElapsedNanos = startNanos == 0 ? 0 : Math.max(0, clock.nanoTime() - startNanos); }

    public SessionMetrics getMetrics() {
        long count = completedCycles.get();
        long elapsed = frozenElapsedNanos;
        Thread current = worker;
        if (current != null && current.isAlive() && startNanos != 0) elapsed = Math.max(0, clock.nanoTime() - startNanos);
        double seconds = elapsed / 1_000_000_000.0;
        return new SessionMetrics(count, seconds, seconds > 0 ? count / seconds : 0);
    }

    public boolean isActive() { Thread current = worker; return current != null && current.isAlive(); }
    public void addListener(Consumer<ExecutionEvent> listener) { listeners.add(listener); }
    public void removeListener(Consumer<ExecutionEvent> listener) { listeners.remove(listener); }
    private void fire(ExecutionEvent event) { listeners.forEach(listener -> listener.accept(event)); }
}
