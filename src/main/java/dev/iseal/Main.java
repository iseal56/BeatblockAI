package dev.iseal;

import dev.iseal.BeatBlockAI.Model.PPOTrainer;

public class Main {
    public static void main(String[] args) {
        //LoggerManager lm = new LoggerManager();
        //lm.setUpLogger();
        PPOTrainer trainer = new PPOTrainer();

        System.out.println("Waiting 5 seconds for you to focus on game window");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Running PPOTrainer");
        trainer.runGame();
    }
}