package dev.iseal.ModInterface;

public class ModInterface {

    protected int misses = 0;
    protected int barelies = 0;
    protected double accuracy = 0.0;
    protected boolean gameEnded = false;

    public void init() {
        JavalinListener listener = new JavalinListener(this);
        listener.start();
    }

    protected void onBareliesRead(String line) {
        this.barelies = Integer.parseInt(line);
    }

    protected void onMissesRead(String line) {
        this.misses = Integer.parseInt(line);
    }

    protected void onAccuracyRead(String line) {
        this.accuracy = Double.parseDouble(line);
    }

    protected void onGameEnd() {
        this.gameEnded = true;
    }

    public int getMisses() {
        return misses;
    }

    public int getBarelies() {
        return barelies;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public boolean hasGameEnded() {
        return gameEnded;
    }

    public void reset() {
        this.misses = 0;
        this.barelies = 0;
        this.accuracy = 0;
        this.gameEnded = false;
    }
}
