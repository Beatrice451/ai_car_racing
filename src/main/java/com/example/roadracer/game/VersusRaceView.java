package com.example.roadracer.game;

import com.example.roadracer.ai.AiModel;
import com.example.roadracer.ai.Sensors;
import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;
import com.example.roadracer.render.CarRenderer;
import com.example.roadracer.render.TrackRenderer;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class VersusRaceView extends BorderPane {
    private final Canvas canvas = new Canvas(1000, 650);
    private final Track track;
    private final AiModel model;
    private final Car playerCar = new Car();
    private final Car aiCar = new Car();
    private final Set<KeyCode> keys = new HashSet<>();
    private final Label hud = new Label();
    private final Runnable onBack;

    private final List<Vec2> checkpoints = new ArrayList<>();
    private double checkpointRadius = 40;
    private int playerCp = 1, aiCp = 1;
    private long startNanos;
    private boolean playerFinished, aiFinished;
    private double playerTime, aiTime;
    private AnimationTimer timer;
    private long lastFrame;

    public VersusRaceView(Track track, AiModel model, Runnable onBack) {
        this.track = track;
        this.model = model;
        this.onBack = onBack;
        setStyle("-fx-background-color: #202328; -fx-padding: 10;");

        Button back = new Button("← Редактор");
        back.setOnAction(e -> { stop(); onBack.run(); });
        hud.setTextFill(Color.WHITE);
        HBox top = new HBox(16, back, hud);
        top.setStyle("-fx-padding: 8; -fx-alignment: center-left;");
        setTop(top);

        canvas.widthProperty().bind(widthProperty().subtract(20));
        canvas.heightProperty().bind(heightProperty().subtract(70));
        canvas.widthProperty().addListener((o, a, b) -> draw());
        canvas.heightProperty().addListener((o, a, b) -> draw());
        setCenter(canvas);

        setFocusTraversable(true);
        setOnKeyPressed(e -> keys.add(e.getCode()));
        setOnKeyReleased(e -> keys.remove(e.getCode()));
        sceneProperty().addListener((obs, o, n) -> { if (n != null) requestFocus(); });

        if (track.getStartPosition() == null || track.getStartAngle() == 0) {
            track.setStartPosition(track.getPoints().get(0));
            track.setStartAngle(track.angleAtStart());
        }

        buildCheckpoints();
        reset();
        startLoop();
    }

    private void buildCheckpoints() {
        checkpoints.clear();
        List<Vec2> pts = track.getPoints();
        double spacing = 12;
        double acc = 0;
        checkpoints.add(pts.get(0));
        for (int i = 1; i < pts.size(); i++) {
            Vec2 a = pts.get(i - 1), b = pts.get(i);
            acc += a.distanceTo(b);
            if (acc >= spacing) { checkpoints.add(b); acc = 0; }
        }
        Vec2 last = pts.get(pts.size() - 1);
        if (!checkpoints.get(checkpoints.size() - 1).equals(last)) checkpoints.add(last);
        checkpointRadius = track.getRoadWidth() * 0.7;
    }

    private void reset() {
        playerCar.reset(track);
        aiCar.reset(track);
        // отодвигаем ИИ чуть назад по линии старта, чтобы не сливался с игроком
        double back = 22;
        aiCar.setPosition(
                aiCar.getX() - Math.cos(aiCar.getAngle()) * back,
                aiCar.getY() - Math.sin(aiCar.getAngle()) * back);
        playerCp = 1;
        aiCp = 1;
        playerFinished = false;
        aiFinished = false;
        playerTime = aiTime = 0;
        startNanos = System.nanoTime();
        lastFrame = 0;
    }

    private void startLoop() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastFrame == 0) lastFrame = now;
                double dt = (now - lastFrame) / 1_000_000_000.0;
                lastFrame = now;
                step(dt);
                updateHud(now);
                draw();
            }
        };
        timer.start();
    }

    private void step(double dt) {
        double elapsed = (System.nanoTime() - startNanos) / 1_000_000_000.0;

        if (!playerFinished) {
            playerCar.update(dt,
                    pressed(KeyCode.W, KeyCode.UP),
                    pressed(KeyCode.S, KeyCode.DOWN),
                    pressed(KeyCode.A, KeyCode.LEFT),
                    pressed(KeyCode.D, KeyCode.RIGHT),
                    track);
            Vec2 pos = new Vec2(playerCar.getX(), playerCar.getY());
            while (playerCp < checkpoints.size()
                    && pos.distanceTo(checkpoints.get(playerCp)) < checkpointRadius) {
                playerCp++;
            }
            if (playerCp >= checkpoints.size()) {
                playerFinished = true;
                playerTime = elapsed;
            }
        }

        if (!aiFinished) {
            int sn = model.sensorCount;
            Vec2 pos = new Vec2(aiCar.getX(), aiCar.getY());
            double[] sensors = Sensors.cast(track, pos, aiCar.getAngle(), sn,
                    model.sensorRange, Math.toRadians(model.sensorSpreadDeg));
            double[] inputs = new double[sn + 1];
            System.arraycopy(sensors, 0, inputs, 0, sn);
            inputs[sn] = Math.abs(aiCar.getSpeed()) / 310.0;
            double[] out = model.brain.forward(inputs);
            double steer = out[0];
            double throttle = 0.5 + 0.5 * out[1];
            aiCar.updateAi(dt, steer, throttle, track);
            while (aiCp < checkpoints.size()
                    && pos.distanceTo(checkpoints.get(aiCp)) < checkpointRadius) {
                aiCp++;
            }
            if (aiCp >= checkpoints.size()) {
                aiFinished = true;
                aiTime = elapsed;
            }
        }
    }

    private boolean pressed(KeyCode a, KeyCode b) { return keys.contains(a) || keys.contains(b); }

    private void updateHud(long now) {
        double t = (now - startNanos) / 1_000_000_000.0;
        int goal = Math.max(1, checkpoints.size() - 1);
        String state;
        if (playerFinished && aiFinished) {
            state = playerTime < aiTime ? "🏆 Вы победили!" : "🤖 ИИ победил.";
        } else if (playerFinished) {
            state = "🏆 Вы финишировали!";
        } else if (aiFinished) {
            state = "🤖 ИИ финишировал.";
        } else {
            state = "";
        }
        hud.setText(String.format("Вы: %d/%d   ИИ: %d/%d   Время %.1f с   %s",
                playerCp - 1, goal, aiCp - 1, goal, t, state));
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        TrackRenderer.draw(g, track, canvas.getWidth(), canvas.getHeight());

        // подсветка следующего чекпоинта игрока — зелёное кольцо
        if (!playerFinished && playerCp < checkpoints.size()) {
            Vec2 cp = checkpoints.get(playerCp);
            g.setStroke(Color.rgb(120, 240, 120, 0.7));
            g.setLineWidth(2);
            g.strokeOval(cp.x() - 10, cp.y() - 10, 20, 20);
        }

        CarRenderer.draw(g, aiCar, Color.rgb(70, 130, 220));   // ИИ — синий
        CarRenderer.draw(g, playerCar, Color.CRIMSON);         // игрок — красный
    }

    public void stop() { if (timer != null) timer.stop(); }
}