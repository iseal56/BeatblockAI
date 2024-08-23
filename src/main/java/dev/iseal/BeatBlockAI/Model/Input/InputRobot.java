package dev.iseal.BeatBlockAI.Model.Input;

import java.awt.*;
import java.awt.image.BufferedImage;

public class InputRobot {
    private Robot robot;

    public InputRobot() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public BufferedImage captureScreen() {
        BufferedImage image = robot.createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())
        );

        // Convert BufferedImage to grayscale
        BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Resize the image to a fixed size and grayscale it
        int width = 128;
        int height = 72;
        BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = resizedImage.createGraphics();
        g2d.drawImage(grayImage, 0, 0, width, height, null);
        g2d.dispose();

        return resizedImage;
    }

}
