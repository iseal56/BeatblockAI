package dev.iseal.BeatBlockAI.Model;

import dev.iseal.BeatBlockAI.Model.Input.InputRobot;
import dev.iseal.BeatBlockAI.Model.Input.OCRUtil;
import dev.iseal.BeatBlockAI.Model.Output.OutputRobot;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PPOTrainer {

    private static final Logger logger = Logger.getLogger(PPOTrainer.class.getName());
    private static final int TARGET_FPS = 40;
    private static final long FRAME_TIME = 1000 / TARGET_FPS;

    private PPOModel model;
    private InputRobot inputRobot;
    private OutputRobot outputRobot;
    private OCRUtil ocrUtil;

    public PPOTrainer()  {
        model = new PPOModel();
        inputRobot = new InputRobot();
        outputRobot = new OutputRobot();
        ocrUtil = new OCRUtil();
        logger.info("PPOTrainer initialized");
    }

    public void runGame() {
        logger.info("Starting game run");
        MultiLayerNetwork network = new MultiLayerNetwork(model.buildModel());
        network.init();
        logger.info("Model initialized");

        ArrayList<INDArray> inputs = new ArrayList<>();
        ArrayList<INDArray> outputs = new ArrayList<>();

        BufferedImage screen;

        while ((screen = inputRobot.captureScreen()) != null && !ocrUtil.doesScreenContainEndGame(screen)) {
            long startTime = System.currentTimeMillis();

            logger.info("Captured screen and checked for end game");

            INDArray input = preprocessImage(screen);
            INDArray output = network.output(input);
            int mouseX = (int) output.getFloat(0);
            int mouseY = (int) output.getFloat(1);
            boolean pressZ = output.getFloat(2) > 0.5;

            logger.info(String.format("Model output: mouseX=%d, mouseY=%d, pressZ=%b", mouseX, mouseY, pressZ));

            outputRobot.moveMouse(mouseX, mouseY);
            if (pressZ) {
                outputRobot.pressZKey();
            }

            inputs.add(input);
            outputs.add(output);

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            long sleepTime = FRAME_TIME - elapsedTime;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Sleep interrupted", e);
                }
            }
        }

        double reward = getReward(screen);
        logger.info("Calculated reward: " + reward);

        for (int i = 0; i < inputs.size(); i++) {
            updateModel(network, inputs.get(i), outputs.get(i), reward);
        }

        logger.info("Model updated, restarting game");
        outputRobot.restartGame();
        runGame();
    }

    private INDArray preprocessImage(BufferedImage image) {
        logger.info("Preprocessing image");
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

    private double getReward(BufferedImage screen) {
        logger.info("Calculating reward");
        return Math.max(1000 - (ocrUtil.getMisses(screen) + ocrUtil.getBarelies(screen) + ocrUtil.getGrade(screen)), 0);
    }

    private void updateModel(MultiLayerNetwork network, INDArray input, INDArray output, double reward) {
        logger.info("Updating model");
        double learningRate = 0.001;
        double discountFactor = 0.99;

        INDArray target = output.mul(reward).mul(discountFactor);
        INDArray loss = target.sub(output);
        INDArray gradient = loss.mul(learningRate);

        network.params().subi(gradient);
    }

}