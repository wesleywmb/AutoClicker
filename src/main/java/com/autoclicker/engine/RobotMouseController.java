package com.autoclicker.engine;

import java.awt.AWTException;
import java.awt.Robot;

public final class RobotMouseController implements MouseController {
    private final Robot robot;

    public RobotMouseController() {
        try {
            robot = new Robot();
            robot.setAutoDelay(0);
            robot.setAutoWaitForIdle(false);
        } catch (AWTException e) {
            throw new IllegalStateException("Não foi possível inicializar o controle do mouse.", e);
        }
    }

    @Override public void move(int x, int y) { robot.mouseMove(x, y); }
    @Override public void press(int buttonMask) { robot.mousePress(buttonMask); }
    @Override public void release(int buttonMask) { robot.mouseRelease(buttonMask); }
}
