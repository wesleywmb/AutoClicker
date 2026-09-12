package com.autoclicker.engine;

public interface MouseController {
    void move(int x, int y);
    void press(int buttonMask);
    void release(int buttonMask);
}
