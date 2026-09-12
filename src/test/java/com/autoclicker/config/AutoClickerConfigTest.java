package com.autoclicker.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AutoClickerConfigTest {
    @Test void rejectsInvalidNumericValues() {
        AutoClickerConfig config = new AutoClickerConfig();
        assertThrows(IllegalArgumentException.class, () -> config.setClickIntervalSeconds(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> config.setClickIntervalSeconds(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> config.setClickIntervalSeconds(0));
        assertThrows(IllegalArgumentException.class, () -> config.setRepeatCount(0));
        assertThrows(IllegalArgumentException.class, () -> config.setRepeatCount(1_000_000));
        assertThrows(IllegalArgumentException.class, () -> config.setStartDelaySeconds(61));
        assertThrows(IllegalArgumentException.class, () -> config.setClicksPerSecond(1001));
    }

    @Test void acceptsNegativeCoordinatesForSecondaryMonitors() {
        AutoClickerConfig config = new AutoClickerConfig();
        config.setFixX(-1920);
        config.setFixY(-200);
        assertEquals(-1920, config.getFixX());
        assertEquals(-200, config.getFixY());
    }
}
