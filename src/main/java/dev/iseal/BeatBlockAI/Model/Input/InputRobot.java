package dev.iseal.BeatBlockAI.Model.Input;

import dev.iseal.BeatBlockAI.Model.PPOTrainer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InputRobot {
    private Robot robot;
    private final Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    private ScheduledExecutorService scheduledExecutorService;
    private BufferedImage currentImage;

    public InputRobot() {
        try {
            robot = new Robot();
            scheduledExecutorService = new ScheduledThreadPoolExecutor(5);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    public synchronized BufferedImage getCaptureScreen() {
        return currentImage;
    }

    public void updateCaptureScreen(boolean update) {
        if (update) {
            scheduledExecutorService.scheduleAtFixedRate(() -> {
                BufferedImage image = robot.createScreenCapture(screenRect);

                // Convert BufferedImage to grayscale
                BufferedImage grayImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
                Graphics g = grayImage.getGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();

                // Resize the image to a fixed size and grayscale it
                int width = 640;
                int height = 360;
                BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g2d = resizedImage.createGraphics();
                g2d.drawImage(grayImage, 0, 0, width, height, null);
                g2d.dispose();

                synchronized (this) {
                    currentImage = resizedImage;
                }

            }, 0, PPOTrainer.FRAME_TIME, TimeUnit.MILLISECONDS);
        } else {
            scheduledExecutorService.shutdown();
        }
    }

    public boolean areImagesEqual(BufferedImage img1, BufferedImage img2) {
        if (img1 == null || img2 == null) {
            return false;
        }

        if (img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight()) {
            return false;
        }

        for (int x = 0; x < img1.getWidth(); x++) {
            for (int y = 0; y < img1.getHeight(); y++) {
                if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
                    return false;
                }
            }
        }

        return true;
    }

}
