package com.autoclicker.engine;

@FunctionalInterface
public interface Sleeper {
    void sleep(long milliseconds) throws InterruptedException;

    static Sleeper threadSleeper() {
        return Thread::sleep;
    }
}
