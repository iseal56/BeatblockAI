package dev.iseal;

import dev.iseal.BeatBlockAI.Model.PPOTrainer;

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
        loop();
    }

    private static void loop() {
        PPOTrainer trainer = new PPOTrainer();
        try {
            trainer.runGame();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("crashed, restarting");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }
            loop();
        }
    }
}