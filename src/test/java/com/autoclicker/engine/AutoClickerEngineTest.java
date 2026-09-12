package com.autoclicker.engine;

import com.autoclicker.config.AutoClickerConfig;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class AutoClickerEngineTest {
    @Test void finiteSessionStopsAtExactLimitWithoutFinalSleepAndFreezesMetrics() throws Exception {
        AutoClickerConfig config = new AutoClickerConfig();
        config.setInfiniteRepeat(false);
        config.setRepeatCount(1);
        AtomicLong nanos = new AtomicLong(100);
        List<Long> sleeps = new ArrayList<>();
        MouseController mouse = new MouseController() {
            public void move(int x, int y) { }
            public void press(int mask) { nanos.addAndGet(1_000_000_000L); }
            public void release(int mask) { }
        };
        AutoClickerEngine engine = new AutoClickerEngine(config, mouse, nanos::get,
                millis -> sleeps.add(millis));
        CountDownLatch stopped = stopLatch(engine);

        assertTrue(engine.start(1));
        assertTrue(stopped.await(2, TimeUnit.SECONDS));
        assertEquals(1, engine.getMetrics().getCompletedActions());
        assertTrue(sleeps.isEmpty(), "não deve aguardar o intervalo depois da última ação");
        double frozen = engine.getMetrics().getElapsedSeconds();
        nanos.addAndGet(10_000_000_000L);
        assertEquals(frozen, engine.getMetrics().getElapsedSeconds());
    }

    @Test void duplicateStartIsRejectedAndStopInterruptsDoubleClick() throws Exception {
        AutoClickerConfig config = new AutoClickerConfig();
        config.setClickType(AutoClickerConfig.ClickType.DOUBLE);
        CountDownLatch betweenClicks = new CountDownLatch(1);
        AtomicInteger presses = new AtomicInteger();
        MouseController mouse = new RecordingMouse(presses, new AtomicInteger());
        Sleeper blockingSleeper = millis -> {
            if (millis == 50) betweenClicks.countDown();
            Thread.sleep(10_000);
        };
        AutoClickerEngine engine = new AutoClickerEngine(config, mouse, System::nanoTime, blockingSleeper);
        CountDownLatch stopped = stopLatch(engine);

        assertTrue(engine.start(8));
        assertFalse(engine.start(9));
        assertTrue(betweenClicks.await(2, TimeUnit.SECONDS));
        engine.stop();
        assertTrue(stopped.await(2, TimeUnit.SECONDS));
        assertEquals(1, presses.get());
        assertEquals(0, engine.getMetrics().getCompletedActions(), "clique duplo interrompido não é uma ação concluída");
    }

    @Test void releaseIsAttemptedWhenPressFails() throws Exception {
        AutoClickerConfig config = new AutoClickerConfig();
        AtomicInteger releases = new AtomicInteger();
        MouseController mouse = new MouseController() {
            public void move(int x, int y) { }
            public void press(int mask) { throw new IllegalStateException("falha simulada"); }
            public void release(int mask) { releases.incrementAndGet(); }
        };
        AutoClickerEngine engine = new AutoClickerEngine(config, mouse, System::nanoTime, millis -> { });
        CountDownLatch failed = new CountDownLatch(1);
        engine.addListener(event -> {
            if (event.getType() == ExecutionEvent.Type.STOPPED
                    && event.getStopReason() == ExecutionEvent.StopReason.FAILED) failed.countDown();
        });
        engine.start(10);
        assertTrue(failed.await(2, TimeUnit.SECONDS));
        assertEquals(1, releases.get());
    }

    private static CountDownLatch stopLatch(AutoClickerEngine engine) {
        CountDownLatch latch = new CountDownLatch(1);
        engine.addListener(event -> { if (event.getType() == ExecutionEvent.Type.STOPPED) latch.countDown(); });
        return latch;
    }

    private static final class RecordingMouse implements MouseController {
        private final AtomicInteger presses;
        private final AtomicInteger releases;
        private RecordingMouse(AtomicInteger presses, AtomicInteger releases) { this.presses = presses; this.releases = releases; }
        public void move(int x, int y) { }
        public void press(int mask) { presses.incrementAndGet(); }
        public void release(int mask) { releases.incrementAndGet(); }
    }
}
