package com.autoclicker.engine;

@FunctionalInterface
public interface MonotonicClock {
    long nanoTime();

    static MonotonicClock system() {
        return System::nanoTime;
    }
}
