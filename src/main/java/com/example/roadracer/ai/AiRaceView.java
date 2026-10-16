package com.example.roadracer.ai;

import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;
import com.example.roadracer.render.CarRenderer;
import com.example.roadracer.render.TrackRenderer;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class AiRaceView extends BorderPane {
    private static final int POP = 40;
    private static final int SENSORS = 5;
    private static final double SENSOR_RANGE = 220;
    private static final double SENSOR_SPREAD = Math.toRadians(140);
    private static final int INPUTS = SENSORS + 1;
    private static final int HIDDEN = 8;
    private static final int OUTPUTS = 2;
    private static final double MAX_TIME = 30.0;
    private static final double DT = 1.0 / 60.0;

    private final Canvas canvas = new Canvas(1000, 650);
    private final Track track;
    private final Runnable onBack;
    private final Random rnd = new Random();
    private final Label hud = new Label();

    private List<Agent> population = new ArrayList<>();
    private int generation = 1;
    private double genTime = 0;
    private int speedMultiplier = 1;
    private AnimationTimer timer;

    public AiRaceView(Track track, Runnable onBack) {
        this.track = track;
        this.onBack = onBack;
        setStyle("-fx-background-color: #202328; -fx-padding: 10;");

        Button back = new Button("← Редактор");
        back.setOnAction(e -> {
            stop();
            onBack.run();
        });
        Button speed = new Button("×10");
        speed.setOnAction(e -> speedMultiplier = speedMultiplier == 1 ? 10 : 1);
        hud.setTextFill(Color.WHITE);
        HBox top = new HBox(16, back, speed, hud);
        top.setStyle("-fx-padding: 8; -fx-alignment: center-left;");
        setTop(top);

        canvas.widthProperty().bind(widthProperty().subtract(20));
        canvas.heightProperty().bind(heightProperty().subtract(70));
        canvas.widthProperty().addListener((o, a, b) -> draw());
        canvas.heightProperty().addListener((o, a, b) -> draw());
        setCenter(canvas);

        initPopulation();
        startLoop();
    }

    private void initPopulation() {
        population.clear();
        for (int i = 0; i < POP; i++) {
            Brain b = new Brain(INPUTS, HIDDEN, OUTPUTS);
            b.randomize(rnd, 0.8);
            population.add(new Agent(b));
        }
        resetAgents();
    }

    private void resetAgents() {
        for (Agent a : population) {
            a.car.reset(track);
            a.maxProgress = 0;
            a.alive = true;
        }
    }

    private void startLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (int s = 0; s < speedMultiplier; s++) stepAll(DT);
                updateHud();
                draw();
            }
        };
        timer.start();
    }

    private void stepAll(double dt) {
        genTime += dt;
        int alive = 0;
        for (Agent a : population) {
            if (!a.alive) continue;
            alive++;
            Vec2 pos = new Vec2(a.car.getX(), a.car.getY());
            double[] sensors = Sensors.cast(track, pos, a.car.getAngle(), SENSORS, SENSOR_RANGE, SENSOR_SPREAD);
            double[] inputs = new double[INPUTS];
            System.arraycopy(sensors, 0, inputs, 0, SENSORS);
            inputs[SENSORS] = Math.abs(a.car.getSpeed()) / 310.0;

            double[] out = a.brain.forward(inputs);
            double steer = out[0];
            double throttle = 0.5 + 0.5 * out[1];

            a.car.updateAi(dt, steer, throttle, track);
            if (a.car.isCrashed()) {
                a.alive = false;
                continue;
            }

            double prog = track.progress(pos);
            if (prog > a.maxProgress) a.maxProgress = prog;
        }
        if (alive == 0 || genTime > MAX_TIME) nextGeneration();
    }

    private void nextGeneration() {
        population.sort(Comparator.comparingDouble((Agent a) -> a.maxProgress).reversed());
        List<Agent> next = new ArrayList<>();
        int elite = Math.max(1, POP / 5);
        for (int i = 0; i < elite; i++) next.add(new Agent(population.get(i).brain.copy()));
        while (next.size() < POP) {
            Brain p1 = tournament();
            Brain p2 = tournament();
            Brain child = Brain.crossover(p1, p2, rnd);
            child.mutate(rnd, 0.15, 0.25);
            next.add(new Agent(child));
        }
        population = next;
        generation++;
        genTime = 0;
        resetAgents();
    }

    private Brain tournament() {
        Brain best = null;
        double bestF = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 3; i++) {
            Agent a = population.get(rnd.nextInt(population.size()));
            if (a.maxProgress > bestF) {
                bestF = a.maxProgress;
                best = a.brain;
            }
        }
        return best;
    }

    private void updateHud() {
        Agent best = population.stream().max(Comparator.comparingDouble(a -> a.maxProgress)).orElse(null);
        long alive = population.stream().filter(a -> a.alive).count();
        int goal = Math.max(0, track.getPoints().size() - 1);
        hud.setText(String.format("Поколение %d   Живых %d/%d   Прогресс %.1f / %d",
                generation, alive, POP, best == null ? 0 : best.maxProgress, goal));
    }

    private void draw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        TrackRenderer.draw(g, track, canvas.getWidth(), canvas.getHeight());

        Agent best = population.stream().max(Comparator.comparingDouble(a -> a.maxProgress)).orElse(null);

        for (Agent a : population) {
            if (!a.alive) continue;
            CarRenderer.draw(g, a.car, Color.rgb(190, 70, 70));
        }

        if (best != null && best.alive) {
            g.setStroke(Color.GOLD);
            g.setLineWidth(2);
            g.strokeOval(best.car.getX() - 24, best.car.getY() - 24, 48, 48);
        }
    }

    public void stop() {
        if (timer != null) timer.stop();
    }
}