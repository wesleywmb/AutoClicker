package com.autoclicker.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.util.prefs.Preferences;
import static org.junit.jupiter.api.Assertions.*;

class ConfigRepositoryTest {
    private Preferences root;
    private ConfigRepository repository;

    @BeforeEach void setUp() {
        root = Preferences.userRoot().node("com/autoclicker/tests/" + UUID.randomUUID());
        repository = new ConfigRepository(root);
    }

    @AfterEach void tearDown() throws Exception {
        Preferences parent = root.parent();
        root.removeNode();
        parent.flush();
    }

    @Test void roundTripPreservesNegativeCoordinatesAndNativeHotkey() {
        AutoClickerConfig source = new AutoClickerConfig();
        source.setFixX(-1200);
        source.setFixY(400);
        source.setHotkey(64, "F6");
        repository.save("jogos", source);

        ConfigRepository.LoadedProfile loaded = repository.load("jogos").orElseThrow();
        assertEquals(-1200, loaded.getConfig().getFixX());
        assertEquals(64, loaded.getConfig().getHotkeyCode());
        assertFalse(loaded.isHotkeyReset());
    }

    @Test void readsLegacyHotkeyAndFallsBackWhenUnknown() {
        root.node("antigo").put("hotkey", "F6");
        assertEquals(64, repository.load("antigo").orElseThrow().getConfig().getHotkeyCode());

        root.node("desconhecido").put("hotkey", "TECLA_QUE_NAO_EXISTE");
        ConfigRepository.LoadedProfile loaded = repository.load("desconhecido").orElseThrow();
        assertTrue(loaded.isHotkeyReset());
        assertEquals(64, loaded.getConfig().getHotkeyCode());
    }

    @Test void rejectsNamesThatPreferencesWouldAlias() {
        assertThrows(IllegalArgumentException.class, () -> repository.save("a/b", new AutoClickerConfig()));
        assertThrows(IllegalArgumentException.class, () -> repository.save("a\\b", new AutoClickerConfig()));
    }
}
