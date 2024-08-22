package dev.iseal;

import dev.iseal.BeatBlockAI.Model.PPOTrainer;
import dev.iseal.Logging.LoggerManager;

public class Main {
    public static void main(String[] args) {
        //LoggerManager lm = new LoggerManager();
        //lm.setUpLogger();
        System.out.println("Waiting 5 seconds for you to focus on game window");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Running PPOTrainer");
        PPOTrainer trainer = new PPOTrainer();
        trainer.runGame();
    }
}