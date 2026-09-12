package com.autoclicker.engine;

public final class ExecutionEvent {
    public enum Type { COUNTDOWN, STARTED, STEP, STOPPED }
    public enum StopReason { COMPLETED, CANCELLED, FAILED }

    private final long sessionId;
    private final Type type;
    private final int value;
    private final StopReason stopReason;
    private final Throwable error;

    private ExecutionEvent(long sessionId, Type type, int value, StopReason stopReason, Throwable error) {
        this.sessionId = sessionId;
        this.type = type;
        this.value = value;
        this.stopReason = stopReason;
        this.error = error;
    }

    public static ExecutionEvent countdown(long id, int seconds) {
        return new ExecutionEvent(id, Type.COUNTDOWN, seconds, null, null);
    }

    public static ExecutionEvent started(long id) {
        return new ExecutionEvent(id, Type.STARTED, 0, null, null);
    }

    public static ExecutionEvent step(long id, int index) {
        return new ExecutionEvent(id, Type.STEP, index, null, null);
    }

    public static ExecutionEvent stopped(long id, StopReason reason, Throwable error) {
        return new ExecutionEvent(id, Type.STOPPED, 0, reason, error);
    }

    public long getSessionId() { return sessionId; }
    public Type getType() { return type; }
    public int getValue() { return value; }
    public StopReason getStopReason() { return stopReason; }
    public Throwable getError() { return error; }
}
