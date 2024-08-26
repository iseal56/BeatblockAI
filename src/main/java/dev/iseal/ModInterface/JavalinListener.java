package dev.iseal.ModInterface;

import io.javalin.Javalin;

import java.sql.SQLSyntaxErrorException;

public class JavalinListener extends Thread {

    Javalin app;
    ModInterface modInterface;

    public JavalinListener(ModInterface modInterface) {
        this.modInterface = modInterface;
        app = Javalin.create();
    }

    public void run() {
        app.post("/misses", ctx -> {
            modInterface.onMissesRead(ctx.body());
        });
        app.post("/barelies", ctx -> {
            modInterface.onBareliesRead(ctx.body());
        });
        app.post("/accuracy", ctx -> {
            modInterface.onAccuracyRead(ctx.body());
        });
        app.post("/game_end", ctx -> {
            modInterface.onGameEnd();
        });
        app.start(2424);
    }

}
