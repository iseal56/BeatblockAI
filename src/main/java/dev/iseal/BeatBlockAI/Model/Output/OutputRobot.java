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
        // normalize mouse position
        robot.mouseMove(0,0);

        // wait for animation to finish and chill out for a second
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        robot.keyPress(KeyEvent.VK_ENTER);
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        robot.keyRelease(KeyEvent.VK_ENTER);
    }

}
