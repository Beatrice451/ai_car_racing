package com.example.roadracer.game;

import com.example.roadracer.model.Track;
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
import java.util.HashSet;
import java.util.Set;

public class RaceView extends BorderPane {
    private final Canvas canvas = new Canvas(1000, 650);
    private final Track track;
    private final Car car = new Car();
    private final Set<KeyCode> keys = new HashSet<>();
    private final Label hud = new Label();
    private final Runnable onBack;
    private AnimationTimer timer;
    private long lastFrame;
    private long startNanos;

    public RaceView(Track track, Runnable onBack) {
        this.track = track; this.onBack = onBack;
        setStyle("-fx-background-color: #202328; -fx-padding: 10;");
        Button back = new Button("← Редактор"); back.setOnAction(e -> stopAndBack());
        hud.setTextFill(Color.WHITE);
        HBox top = new HBox(16, back, hud); top.setStyle("-fx-padding: 8; -fx-alignment: center-left;"); setTop(top);
        canvas.widthProperty().bind(widthProperty().subtract(20)); canvas.heightProperty().bind(heightProperty().subtract(70));
        canvas.widthProperty().addListener((o,a,b)->draw()); canvas.heightProperty().addListener((o,a,b)->draw());
        setCenter(canvas);
        setFocusTraversable(true);
        setOnKeyPressed(e -> keys.add(e.getCode())); setOnKeyReleased(e -> keys.remove(e.getCode()));
        car.reset(track); startNanos = System.nanoTime(); startLoop();
        sceneProperty().addListener((obs, oldScene, newScene) -> { if (newScene != null) requestFocus(); });
    }

    private void startLoop() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastFrame == 0) lastFrame = now;
                double dt = (now - lastFrame) / 1_000_000_000.0; lastFrame = now;
                car.update(dt, pressed(KeyCode.W, KeyCode.UP), pressed(KeyCode.S, KeyCode.DOWN), pressed(KeyCode.A, KeyCode.LEFT), pressed(KeyCode.D, KeyCode.RIGHT), track);
                hud.setText(String.format("Скорость: %3.0f   Время: %.1f с   Управление: WASD / стрелки", Math.abs(car.getSpeed()), (now - startNanos) / 1_000_000_000.0));
                draw();
            }
        };
        timer.start();
    }
    private boolean pressed(KeyCode a, KeyCode b) { return keys.contains(a) || keys.contains(b); }
    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        TrackRenderer.draw(g, track, canvas.getWidth(), canvas.getHeight());
        CarRenderer.draw(g, car);
    }
    private void stopAndBack() { if (timer != null) timer.stop(); onBack.run(); }
    public void stop() { if (timer != null) timer.stop(); }
}
