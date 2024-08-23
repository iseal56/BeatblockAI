package dev.iseal.BeatBlockAI.Model;

import dev.iseal.BeatBlockAI.Model.Input.InputRobot;
import dev.iseal.BeatBlockAI.Model.Output.OutputRobot;
import dev.iseal.ModInterface.ModInterface;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PPOTrainer {

    private static final Logger logger = Logger.getLogger(PPOTrainer.class.getName());
    private static final int TARGET_FPS = 30;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;
    private ExecutorService executorService;

    private PPOModel model;
    private InputRobot inputRobot;
    private OutputRobot outputRobot;
    private ModInterface modInterface;
    private MultiLayerNetwork network;

    /*
    static {
        // Ensure ND4J uses the CUDA backend
        try {
            Nd4jBackend.load();
        } catch (Nd4jBackend.NoAvailableBackendException e) {
            throw new RuntimeException(e);
        }
    } */

    public PPOTrainer()  {
        model = new PPOModel();
        inputRobot = new InputRobot();
        outputRobot = new OutputRobot();
        modInterface = new ModInterface();
        network = new MultiLayerNetwork(model.buildModel());
        network.init();
        modInterface.init();
        executorService = Executors.newFixedThreadPool(20);
        logger.info("PPOTrainer initialized");
    }

    public void runGame() {
        logger.info("Starting game run");

        INDArray lastInput = null;
        INDArray lastOutput = null;

        BufferedImage screen;
        double oldAccuracy = 0;

        while ((screen = inputRobot.captureScreen()) != null && !modInterface.hasGameEnded()) {
            long startTime = System.currentTimeMillis();

            INDArray input = preprocessImage(screen);

            INDArray output = network.output(input);
            int mouseX = (int) output.getFloat(0);
            int mouseY = (int) output.getFloat(1);
            boolean pressZ = output.getFloat(2) > 0.5;

            // Normalize coordinates to screen bounds
            mouseX = Math.max(0, Math.min(mouseX, Toolkit.getDefaultToolkit().getScreenSize().width - 1));
            mouseY = Math.max(0, Math.min(mouseY, Toolkit.getDefaultToolkit().getScreenSize().height - 1));

            System.out.printf("Model output: mouseX=%d, mouseY=%d, pressZ=%b%n", mouseX, mouseY, pressZ);

            outputRobot.moveMouse(mouseX, mouseY);
            if (pressZ) {
                outputRobot.pressZKey();
            }

                if (modInterface.getAccuracy() < oldAccuracy) {
                    System.out.println("Accuracy decreased, penalizing");
                    updateModel(network, input, output, -100);
                    continue;
                } else {
                    System.out.println("Accuracy increased, rewarding");
                    updateModel(network, input, output, 100);
                }
                oldAccuracy = modInterface.getAccuracy();

            lastInput = input;
            lastOutput = output;

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            long sleepTime = FRAME_TIME - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Sleep interrupted", e);
                }
            } else {
                logger.warning("Too slow, not sleeping!");
            }
        }

        double reward = getReward();
        System.out.println("Calculated reward: " + reward);

        System.out.println("Game ended, updating model");
        updateModel(network, lastInput, lastOutput, reward);

        System.out.println("Model updated, restarting game");
        outputRobot.restartGame();
        modInterface.reset();
        runGame();
    }

    private INDArray preprocessImage(BufferedImage image) {
        int width = 128;
        int height = 72;

        int[] pixels = new int[width * height];
        image.getRaster().getPixels(0, 0, width, height, pixels);
        float[] normalizedPixels = new float[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            normalizedPixels[i] = pixels[i] / 255.0f;
        }

        return Nd4j.create(normalizedPixels, new int[]{1, 1, height, width});
    }

    private double getReward() {
        return modInterface.getAccuracy() - (modInterface.getMisses() + modInterface.getBarelies());
    }

    private void updateModel(MultiLayerNetwork network, INDArray input, INDArray output, double reward) {
        logger.info("Updating model, reward: " + reward);
        double discountFactor = 0.99;

        executorService.submit(() -> {
            // Calculate the target value
            INDArray target = output.mul(reward).mul(discountFactor);
            logger.info("Target for training: " + target);

            // Create a dataset with the input and target
            DataSet dataSet = new DataSet(input, target);

            // Fit the model with the dataset
            network.fit(dataSet);
            logger.info("Model updated");
        });
    }

}