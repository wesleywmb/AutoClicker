package com.autoclicker.macro;

import com.autoclicker.config.AutoClickerConfig.ClickType;
import com.autoclicker.config.AutoClickerConfig.MouseButton;
import java.util.Objects;

public final class ClickAction {
    public enum ActionType {
        CLICK("Clique"), WAIT("Espera"), MOVE("Mover");
        private final String displayName;
        ActionType(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
        @Override public String toString() { return displayName; }
    }

    private final ActionType actionType;
    private final int x;
    private final int y;
    private final MouseButton mouseButton;
    private final ClickType clickType;
    private final int waitMs;

    public ClickAction(int x, int y, MouseButton button, ClickType clickType) {
        this(ActionType.CLICK, x, y, Objects.requireNonNull(button), Objects.requireNonNull(clickType), 0);
    }
    public ClickAction(int waitMs) { this(ActionType.WAIT, 0, 0, MouseButton.LEFT, ClickType.SINGLE, requireWait(waitMs)); }
    public ClickAction(int x, int y) { this(ActionType.MOVE, x, y, MouseButton.LEFT, ClickType.SINGLE, 0); }

    private ClickAction(ActionType type, int x, int y, MouseButton button, ClickType clickType, int waitMs) {
        this.actionType = type; this.x = x; this.y = y; this.mouseButton = button; this.clickType = clickType; this.waitMs = waitMs;
    }
    private static int requireWait(int value) {
        if (value < 0) throw new IllegalArgumentException("A espera não pode ser negativa.");
        return value;
    }

    public ActionType getActionType() { return actionType; }
    public int getX() { return x; }
    public int getY() { return y; }
    public MouseButton getMouseButton() { return mouseButton; }
    public ClickType getClickType() { return clickType; }
    public int getWaitMs() { return waitMs; }

    public String getSummary() {
        switch (actionType) {
            case CLICK: return String.format("Clique %s (%s) em (%d, %d)", clickType.getDisplayName(), mouseButton.getDisplayName(), x, y);
            case WAIT: return String.format("Espera %d ms", waitMs);
            case MOVE: return String.format("Mover para (%d, %d)", x, y);
            default: throw new IllegalStateException("Ação desconhecida.");
        }
    }
}
