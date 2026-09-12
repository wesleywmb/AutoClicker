package com.autoclicker.presenter;

import com.autoclicker.engine.SessionMetrics;
import com.autoclicker.engine.ExecutionEvent;

public interface IAutoClickerView {
    void onExecutionStateChanged(ExecutionState state, boolean macroMode);
    void onStatusTick(SessionMetrics metrics, long total, boolean infinite, boolean macroMode);
    void onCountdown(int secondsLeft);
    void onActionIndexChanged(int index);
    void onExecutionFinished(ExecutionEvent.StopReason reason);
    void onExecutionError(String message, Throwable error);
}
