package dev.iseal.BeatBlockAI.Model.Output;

import java.awt.*;
import java.awt.event.KeyEvent;

public class OutputRobot {

    private Robot robot;

    public OutputRobot() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public void moveMouse(int x, int y) {
        robot.mouseMove(x, y);
    }

    public void pressZKey() {
        robot.keyPress(KeyEvent.VK_Z);
        robot.keyRelease(KeyEvent.VK_Z);
    }

    public void restartGame() {
        robot.keyPress(KeyEvent.VK_DOWN);
        robot.keyRelease(KeyEvent.VK_DOWN);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

}
