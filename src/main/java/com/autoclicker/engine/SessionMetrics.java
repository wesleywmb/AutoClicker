package com.autoclicker.engine;

public final class SessionMetrics {
    public static final SessionMetrics EMPTY = new SessionMetrics(0, 0, 0);

    private final long completedActions;
    private final double elapsedSeconds;
    private final double actionsPerSecond;

    public SessionMetrics(long completedActions, double elapsedSeconds, double actionsPerSecond) {
        this.completedActions = completedActions;
        this.elapsedSeconds = elapsedSeconds;
        this.actionsPerSecond = actionsPerSecond;
    }

    public long getCompletedActions() { return completedActions; }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public double getActionsPerSecond() { return actionsPerSecond; }
}
