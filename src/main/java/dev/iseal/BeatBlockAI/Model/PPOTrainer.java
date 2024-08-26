package dev.iseal.BeatBlockAI.Model;

import dev.iseal.BeatBlockAI.Model.Input.InputRobot;
import dev.iseal.BeatBlockAI.Model.Output.OutputRobot;
import dev.iseal.ModInterface.ModInterface;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.ImagePreProcessingScaler;
import org.nd4j.linalg.factory.Nd4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class PPOTrainer {

    private static final Logger logger = Logger.getLogger(PPOTrainer.class.getName());
    private static final int TARGET_FPS = 40;
    public static final long FRAME_TIME = 1000 / TARGET_FPS;
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    private final BlockingQueue<INDArray> inputs = new LinkedBlockingQueue<>();
    private final BlockingQueue<INDArray> outputs = new LinkedBlockingQueue<>();
    private final BlockingQueue<Double> rewards = new LinkedBlockingQueue<>();

    private final InputRobot inputRobot;
    private final OutputRobot outputRobot;
    private final ModInterface modInterface;
    private final MultiLayerNetwork network;

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
        inputRobot = new InputRobot();
        outputRobot = new OutputRobot();
        modInterface = new ModInterface();
        File netFile = new File(Paths.get("").toAbsolutePath().toString()+"/net/network.zip");
        if (netFile.exists()) {
            System.out.println("Loading network from file");
            try {
                network = MultiLayerNetwork.load(netFile, true);
                network.init();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("Creating new network");
            PPOModel model = new PPOModel();
            network = new MultiLayerNetwork(model.buildModel());
            network.init();
        }
        modInterface.init();
        executorService = Executors.newFixedThreadPool(5);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        logger.info("PPOTrainer initialized");
    }

    public void runGame() {
        logger.info("Starting game run");

        inputRobot.updateCaptureScreen(true);
        while (inputRobot.getCaptureScreen() == null) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        BufferedImage screen;

        while (!modInterface.hasGameEnded()) {
            screen = inputRobot.getCaptureScreen();
            long startTime = System.currentTimeMillis();

            INDArray input = preprocessImage(screen);

            INDArray output = network.output(input,true);
            System.out.println("Output: " + output);
            float outMouseX = output.getFloat(0);
            float outMouseY = output.getFloat(1);
            boolean pressZ = output.getFloat(2) > 0.5f;

            // Normalize coordinates to screen bounds,
            // assuming that the screen is 1080p
            // but the mouseX and mouseY values are between 0 and 1
            int mouseX = (int) (outMouseX * 1920f);
            int mouseY = (int) (outMouseY * 1080f);


            outputRobot.moveMouse(mouseX, mouseY);
            if (pressZ) {
                outputRobot.pressZKey();
            }

            updateModel(input, output, modInterface.getAccuracy()-50);

            long endTime = System.currentTimeMillis();
            long elapsedTime = endTime - startTime;
            long sleepTime = FRAME_TIME - elapsedTime;
            if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    logger.warning("Too slow, not sleeping!");
            }
        }

        inputRobot.updateCaptureScreen(false);

        double reward = getReward();
        System.out.println("Calculated reward: " + reward);

        double discountFactor = 0.99;
        //BlockingQueue<DataSet> set = new ArrayBlockingQueue<>(inputs.size());

        int initialSize = inputs.size();

        logger.info("Updating model with " + initialSize + " datasets");

        for (int i = 0; i < initialSize; i++) {
            //use executorservice to parallelize training
            executorService.submit(() -> {
                long allocated = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                long presumableFree = 5L*1024*1024*1024 - allocated;
                if (presumableFree < 3L*1024L*1024L*1024L) {
                    System.gc();
                    while (presumableFree < 3L*1024L*1024L*1024L) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        presumableFree = 5L*1024*1024*1024 - (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
                    }
                }
                INDArray input = inputs.poll();
                INDArray output = outputs.poll();
                double r = rewards.poll();

                // Calculate the target value using the current output and reward
                double targetValue = r + discountFactor * output.getDouble(0);
                double targetValue2 = r + discountFactor * output.getDouble(1);
                double targetValue3 = r + discountFactor * output.getDouble(2);
                INDArray target = Nd4j.create(new double[]{targetValue, targetValue2, targetValue3}, new int[]{1, 3});

                // Create a dataset with the input and target
                DataSet dataSet = new DataSet(input, target);
                network.fit(dataSet);
/*                set.add(dataSet);
                done.getAndIncrement();
                logger.info("Added dataset to queue");*/
            });
        }

        waitForExecutorService();
/*
        while (!set.isEmpty()) {
            DataSet dataSet = set.poll();
            network.fit(dataSet);
            logger.info("Model updated");
        }
*/

    }

    private INDArray preprocessImage(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        // Create an array to hold the pixel data
        int[] pixels = new int[width * height];

        // Ensure the pixel array is correctly filled
        image.getRaster().getPixels(0, 0, width, height, pixels);

        // Convert the pixel array to float array
        float[] floatPixels = new float[pixels.length];
        for (int i = 0; i < pixels.length; i++) {
            floatPixels[i] = pixels[i];
        }

        // Create an INDArray from the pixel data
        return Nd4j.create(floatPixels, new int[]{1, 1, height, width});
    }

    private double getReward() {
        return modInterface.getAccuracy() - (modInterface.getMisses() + modInterface.getBarelies());
    }

    private void updateModel(INDArray input, INDArray output, double reward) {
        logger.info("Updating model, reward: " + reward);

        inputs.add(input);
        outputs.add(output);
        rewards.add(reward);
    }

    public void save() throws IOException {
        File netFile = new File(Paths.get("").toAbsolutePath().toString()+"/net/network.zip");
        if (!netFile.exists()) {
            netFile.getParentFile().mkdirs();
            netFile.createNewFile();
        }
        network.save(netFile, true);
        logger.info("PPOTrainer shut down");
    }

    private void waitForExecutorService() {
        executorService.shutdown();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                logger.info("Waiting for model update to finish");
                if (executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduledExecutorService.shutdown();
                    resetForNextGame();
                    runGame();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void resetForNextGame() {
        inputs.clear();
        outputs.clear();
        rewards.clear();

        try {
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        executorService = Executors.newFixedThreadPool(5);
        scheduledExecutorService = Executors.newScheduledThreadPool(1);

        System.out.println("Model updated, restarting game");
        outputRobot.restartGame();
        modInterface.reset();
    }
}