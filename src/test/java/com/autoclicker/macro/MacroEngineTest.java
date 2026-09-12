package com.autoclicker.macro;

import com.autoclicker.config.AutoClickerConfig;
import com.autoclicker.engine.ExecutionEvent;
import com.autoclicker.engine.MouseController;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class MacroEngineTest {
    @Test void interruptedCycleIsNotCounted() throws Exception {
        AutoClickerConfig config = new AutoClickerConfig();
        MacroSequence sequence = new MacroSequence();
        sequence.add(new ClickAction(10, 10));
        sequence.add(new ClickAction(5000));
        CountDownLatch waiting = new CountDownLatch(1);
        MouseController mouse = new MouseController() {
            public void move(int x, int y) { }
            public void press(int mask) { }
            public void release(int mask) { }
        };
        MacroEngine engine = new MacroEngine(config, mouse, System::nanoTime, millis -> {
            if (millis == 5000) waiting.countDown();
            Thread.sleep(millis);
        });
        CountDownLatch stopped = new CountDownLatch(1);
        engine.addListener(event -> { if (event.getType() == ExecutionEvent.Type.STOPPED) stopped.countDown(); });

        assertTrue(engine.start(1, sequence));
        assertTrue(waiting.await(2, TimeUnit.SECONDS));
        engine.stop();
        assertTrue(stopped.await(2, TimeUnit.SECONDS));
        assertEquals(0, engine.getMetrics().getCompletedActions());
    }
}
