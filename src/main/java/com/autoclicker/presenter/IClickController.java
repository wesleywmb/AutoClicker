package com.autoclicker.presenter;

public interface IClickController {
    void toggleClicking();
    int getHotkeyCode();
    boolean isActivationAllowed();
}
