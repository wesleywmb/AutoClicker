package com.autoclicker.config;

import java.awt.event.InputEvent;

public class AutoClickerConfig {
    
    private double clickIntervalSeconds;
    private MouseButton mouseButton;
    private ClickType clickType;
    private int repeatCount;
    private boolean infiniteRepeat;
    private String hotkeyActivation;
    private int hotkeyCode;
    private int startDelaySeconds;
    private boolean useFixedPosition;
    private int fixX;
    private int fixY;

    public static final double MIN_INTERVAL = 0.001;
    public static final double MAX_INTERVAL = 60.0;
    public static final int MAX_REPEAT_COUNT = 999_999;
    public static final int MAX_START_DELAY = 60;
    public static final int DEFAULT_HOTKEY_CODE = 64; // NativeKeyEvent.VC_F6
    
    /**
     * Enumeração para botões do mouse
     */
    public enum MouseButton {
        LEFT("Esquerdo", InputEvent.BUTTON1_DOWN_MASK),
        RIGHT("Direito", InputEvent.BUTTON3_DOWN_MASK),
        MIDDLE("Meio", InputEvent.BUTTON2_DOWN_MASK);
        
        private final String displayName;
        private final int mask;
        
        MouseButton(String displayName, int mask) {
            this.displayName = displayName;
            this.mask = mask;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getMask() {
            return mask;
        }
    }
    
    /**
     * Enumeração para tipos de clique
     */
    public enum ClickType {
        SINGLE("Simples", 1),
        DOUBLE("Duplo", 2);
        
        private final String displayName;
        private final int clickCount;
        
        ClickType(String displayName, int clickCount) {
            this.displayName = displayName;
            this.clickCount = clickCount;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public int getClickCount() {
            return clickCount;
        }
    }
    
    public AutoClickerConfig() {
        this.clickIntervalSeconds = 0.2;
        this.mouseButton = MouseButton.LEFT;
        this.clickType = ClickType.SINGLE;
        this.hotkeyActivation = "F6";
        this.hotkeyCode = DEFAULT_HOTKEY_CODE;
        this.repeatCount = 100;
        this.infiniteRepeat = true;
        this.startDelaySeconds = 0;
        this.useFixedPosition = false;
        this.fixX = 0;
        this.fixY = 0;
    }
    
    // ==================== Getters e Setters com Validação ====================
    
    public double getClickIntervalSeconds() {
        return clickIntervalSeconds;
    }
    
    public void setClickIntervalSeconds(double intervalSeconds) {
        if (!Double.isFinite(intervalSeconds) || intervalSeconds < MIN_INTERVAL || intervalSeconds > MAX_INTERVAL) {
            throw new IllegalArgumentException("O intervalo deve estar entre 0,001 e 60 segundos.");
        }
        this.clickIntervalSeconds = intervalSeconds;
    }
    
    public int getClickIntervalMillis() {
        return (int) (clickIntervalSeconds * 1000);
    }
    
    public void setClickIntervalMillis(int millis) {
        setClickIntervalSeconds(millis / 1000.0);
    }
    
    public MouseButton getMouseButton() {
        return mouseButton;
    }
    
    public void setMouseButton(MouseButton mouseButton) {
        if (mouseButton == null) throw new IllegalArgumentException("Selecione um botão do mouse.");
        this.mouseButton = mouseButton;
    }
    
    public ClickType getClickType() {
        return clickType;
    }
    
    public void setClickType(ClickType clickType) {
        if (clickType == null) throw new IllegalArgumentException("Selecione um tipo de clique.");
        this.clickType = clickType;
    }
    
    public String getHotkeyActivation() {
        return hotkeyActivation;
    }
    
    public int getHotkeyCode() { return hotkeyCode; }

    public void setHotkey(int code, String displayName) {
        if (code <= 0 || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Tecla de atalho inválida.");
        }
        this.hotkeyCode = code;
        this.hotkeyActivation = displayName;
    }
    
    public int getRepeatCount() {
        return repeatCount;
    }
    
    public void setRepeatCount(int repeatCount) {
        if (repeatCount < 1 || repeatCount > MAX_REPEAT_COUNT) {
            throw new IllegalArgumentException("A repetição deve estar entre 1 e 999.999.");
        }
        this.repeatCount = repeatCount;
    }
    
    public boolean isInfiniteRepeat() {
        return infiniteRepeat;
    }
    
    public void setInfiniteRepeat(boolean infiniteRepeat) {
        this.infiniteRepeat = infiniteRepeat;
    }
    
    public int getStartDelaySeconds() {
        return startDelaySeconds;
    }

    public void setStartDelaySeconds(int seconds) {
        if (seconds < 0 || seconds > MAX_START_DELAY) {
            throw new IllegalArgumentException("O atraso deve estar entre 0 e 60 segundos.");
        }
        this.startDelaySeconds = seconds;
    }

    public boolean isUseFixedPosition() {
        return useFixedPosition;
    }

    public void setUseFixedPosition(boolean useFixedPosition) {
        this.useFixedPosition = useFixedPosition;
    }

    public int getFixX() {
        return fixX;
    }

    public void setFixX(int fixX) {
        this.fixX = fixX;
    }

    public int getFixY() {
        return fixY;
    }

    public void setFixY(int fixY) {
        this.fixY = fixY;
    }

    public void setClicksPerSecond(int cps) {
        if (cps < 1 || cps > 1000) throw new IllegalArgumentException("A taxa deve estar entre 1 e 1.000 ações/s.");
        setClickIntervalSeconds(1.0 / cps);
    }
    
    public int getClicksPerSecond() {
        return (int) Math.round(1.0 / clickIntervalSeconds);
    }

    public AutoClickerConfig copy() {
        AutoClickerConfig copy = new AutoClickerConfig();
        copy.copyFrom(this);
        return copy;
    }

    public void copyFrom(AutoClickerConfig other) {
        if (other == null) throw new IllegalArgumentException("Configuração ausente.");
        setClickIntervalSeconds(other.getClickIntervalSeconds());
        setMouseButton(other.getMouseButton());
        setClickType(other.getClickType());
        setRepeatCount(other.getRepeatCount());
        setInfiniteRepeat(other.isInfiniteRepeat());
        setHotkey(other.getHotkeyCode(), other.getHotkeyActivation());
        setStartDelaySeconds(other.getStartDelaySeconds());
        setUseFixedPosition(other.isUseFixedPosition());
        setFixX(other.getFixX());
        setFixY(other.getFixY());
    }

    public void validate() {
        if (!Double.isFinite(clickIntervalSeconds) || clickIntervalSeconds < MIN_INTERVAL || clickIntervalSeconds > MAX_INTERVAL) {
            throw new IllegalArgumentException("O intervalo deve estar entre 0,001 e 60 segundos.");
        }
        if (mouseButton == null || clickType == null) throw new IllegalArgumentException("Configuração de mouse inválida.");
        if (repeatCount < 1 || repeatCount > MAX_REPEAT_COUNT) throw new IllegalArgumentException("Repetição inválida.");
        if (startDelaySeconds < 0 || startDelaySeconds > MAX_START_DELAY) throw new IllegalArgumentException("Atraso inválido.");
        if (hotkeyCode <= 0 || hotkeyActivation == null || hotkeyActivation.isBlank()) throw new IllegalArgumentException("Tecla de atalho inválida.");
    }
}
