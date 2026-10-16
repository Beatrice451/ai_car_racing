package com.example.roadracer.ai;

import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;
import com.example.roadracer.render.CarRenderer;
import com.example.roadracer.render.TrackRenderer;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;

public class AiRaceView extends BorderPane {
    private static final int OUTPUTS = 2;
    private static final double DT = 1.0 / 60.0;

    // --- ручки, которые крутят участники ---
    // Мозг
    private int popSize = 40;
    private int sensorCount = 5;
    private int hiddenSize = 8;
    private double sensorRange = 220;
    private double sensorSpreadDeg = 140;

    // Эволюция
    private double mutationRate = 0.15;
    private double mutationPower = 0.25;
    private double elitePercent = 20;
    private int tournamentSize = 3;

    // Симуляция
    private double maxTime = 30.0;
    private double timePenalty = 0.0;
    private int speedMultiplier = 1;

    private final Canvas canvas = new Canvas();
    private final Canvas graph = new Canvas(260, 120);
    private final Track track;
    private final Runnable onBack;
    private final Random rnd = new Random();
    private Consumer<AiModel> onBestUpdate;
    private Brain bestBrainEver;
    private final Label modelStatus = new Label("Модель: не сохранена");

    private final Label hud = new Label();
    private final Label stats = new Label();

    private List<Agent> population = new ArrayList<>();
    private List<Vec2> checkpoints = new ArrayList<>();
    private double checkpointRadius = 40;
    private int generation = 1;
    private double genTime = 0;
    private AnimationTimer timer;

    private final List<Double> history = new ArrayList<>();
    private double bestEver = 0;
    private int bestEverGen = 1;

    public AiRaceView(Track track, Runnable onBack, Consumer<AiModel> onBestUpdate) {
        this.track = track;
        this.onBack = onBack;
        this.onBestUpdate = onBestUpdate;
        setStyle("-fx-background-color: #202328; -fx-padding: 10;");

        Button back = new Button("← Редактор");
        back.setOnAction(e -> {
            stop();
            onBack.run();
        });
        Button speed = new Button("×10");
        speed.setOnAction(e -> speedMultiplier = speedMultiplier == 1 ? 10 : 1);
        Button reset = new Button("Заново");
        reset.setOnAction(e -> hardReset());
        hud.setTextFill(Color.WHITE);
        HBox top = new HBox(12, back, speed, reset, hud);
        top.setStyle("-fx-padding: 8; -fx-alignment: center-left;");
        setTop(top);

        VBox side = buildSidePanel();
        ScrollPane scroll = new ScrollPane(side);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(310);
        scroll.setMinWidth(310);
        scroll.setMaxWidth(310);
        scroll.setStyle("-fx-background: #26292f; -fx-background-color: #26292f;");

        Pane canvasHolder = new Pane(canvas);
        canvasHolder.setMinSize(0, 0);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(canvasHolder.widthProperty());
        clip.heightProperty().bind(canvasHolder.heightProperty());
        canvasHolder.setClip(clip);
        HBox.setHgrow(canvasHolder, Priority.ALWAYS);

        canvas.widthProperty().bind(canvasHolder.widthProperty());
        canvas.heightProperty().bind(canvasHolder.heightProperty());
        canvas.widthProperty().addListener((o, a, b) -> draw());
        canvas.heightProperty().addListener((o, a, b) -> draw());

        HBox main = new HBox(8, canvasHolder, scroll);
        setCenter(main);

        // автостарт, если пользователь забыл «Старт здесь»
        if (track.getStartPosition() == null || track.getStartAngle() == 0) {
            track.setStartPosition(track.getPoints().get(0));
            track.setStartAngle(track.angleAtStart());
        }

        buildCheckpoints();
        initPopulation();
        startLoop();
    }

    // ---------- Чекпоинты ----------

    private void buildCheckpoints() {
        checkpoints.clear();
        List<Vec2> pts = track.getPoints();
        int step = Math.max(1, pts.size() / 120);
        for (int i = 0; i < pts.size(); i += step) checkpoints.add(pts.get(i));
        Vec2 last = pts.get(pts.size() - 1);
        if (!checkpoints.get(checkpoints.size() - 1).equals(last)) checkpoints.add(last);
        checkpointRadius = Math.max(30, track.getRoadWidth() * 0.7);
    }

    // ---------- UI ----------

    private VBox buildSidePanel() {
        Label graphTitle = new Label("Прогресс по поколениям");
        graphTitle.setTextFill(Color.WHITE);
        graphTitle.setStyle("-fx-font-weight: bold;");
        stats.setTextFill(Color.rgb(210, 210, 210));
        stats.setStyle("-fx-font-family: monospace; -fx-font-size: 11;");

        VBox brain = section("Мозг",
                sliderInt("Популяция", 10, 120, popSize, v -> popSize = v),
                sliderInt("Сенсоры", 3, 11, sensorCount, v -> sensorCount = v),
                sliderInt("Нейроны", 4, 24, hiddenSize, v -> hiddenSize = v),
                slider("Дальность", 60, 500, sensorRange, v -> sensorRange = v),
                slider("Обзор °", 30, 270, sensorSpreadDeg, v -> sensorSpreadDeg = v)
        );

        VBox evo = section("Эволюция",
                slider("Мутация %", 1, 60, mutationRate * 100, v -> mutationRate = v / 100.0),
                slider("Сила мутации", 1, 100, mutationPower * 100, v -> mutationPower = v / 100.0),
                slider("Элита %", 0, 50, elitePercent, v -> elitePercent = v),
                sliderInt("Турнир", 2, 8, tournamentSize, v -> tournamentSize = v)
        );

        VBox sim = section("Симуляция",
                slider("Время поколения", 5, 60, maxTime, v -> maxTime = v),
                slider("Штраф времени ×100", 0, 20, timePenalty * 100, v -> timePenalty = v / 100.0)
        );

        Label hint = new Label("«Популяция», «Сенсоры», «Нейроны»\nприменятся после кнопки «Заново».");
        hint.setTextFill(Color.rgb(160, 160, 160));
        hint.setStyle("-fx-font-size: 10;");

        VBox box = new VBox(10, graphTitle, graph, stats, modelStatus, brain, evo, sim, hint);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: #26292f;");
        return box;
    }

    private VBox section(String title, VBox... items) {
        Label l = new Label(title);
        l.setTextFill(Color.rgb(180, 200, 255));
        l.setStyle("-fx-font-weight: bold; -fx-font-size: 11;");
        VBox v = new VBox(4);
        v.getChildren().add(l);
        v.getChildren().addAll(items);
        return v;
    }

    private VBox slider(String name, double min, double max, double value, DoubleConsumer onChange) {
        Label label = new Label(name + ": " + String.format("%.0f", value));
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 11;");
        Slider s = new Slider(min, max, value);
        s.valueProperty().addListener((o, a, b) -> {
            onChange.accept(b.doubleValue());
            label.setText(name + ": " + String.format("%.0f", b.doubleValue()));
        });
        return new VBox(1, label, s);
    }

    private VBox sliderInt(String name, int min, int max, int value, IntConsumer onChange) {
        Label label = new Label(name + ": " + value);
        label.setTextFill(Color.WHITE);
        label.setStyle("-fx-font-size: 11;");
        Slider s = new Slider(min, max, value);
        s.setSnapToTicks(true);
        s.setMajorTickUnit(1);
        s.setMinorTickCount(0);
        s.setBlockIncrement(1);
        s.valueProperty().addListener((o, a, b) -> {
            int v = (int) Math.round(b.doubleValue());
            onChange.accept(v);
            label.setText(name + ": " + v);
        });
        return new VBox(1, label, s);
    }

    // ---------- GA ----------

    private void initPopulation() {
        population.clear();
        int inputs = sensorCount + 1;
        for (int i = 0; i < popSize; i++) {
            Brain b = new Brain(inputs, hiddenSize, OUTPUTS);
            b.randomize(rnd, 0.8);
            population.add(new Agent(b));
        }
        resetAgents();
    }

    private void hardReset() {
        generation = 1;
        genTime = 0;
        bestEver = 0;
        bestEverGen = 1;
        history.clear();
        initPopulation();
    }

    private void resetAgents() {
        for (Agent a : population) {
            a.car.reset(track);
            a.maxProgress = 0;
            a.alive = true;
            a.nextCheckpoint = 1;
        }
    }

    private void startLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                for (int s = 0; s < speedMultiplier; s++) stepAll(DT);
                updateHud();
                draw();
                drawGraph();
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
            int sn = a.brain.in - 1;
            Vec2 pos = new Vec2(a.car.getX(), a.car.getY());
            double[] sensors = Sensors.cast(track, pos, a.car.getAngle(), sn,
                    sensorRange, Math.toRadians(sensorSpreadDeg));
            double[] inputs = new double[a.brain.in];
            System.arraycopy(sensors, 0, inputs, 0, sn);
            inputs[sn] = Math.abs(a.car.getSpeed()) / 310.0;

            double[] out = a.brain.forward(inputs);
            double steer = out[0];
            double throttle = 0.5 + 0.5 * out[1];

            a.car.updateAi(dt, steer, throttle, track);
            if (a.car.isCrashed()) {
                a.alive = false;
                continue;
            }

            // последовательные чекпоинты
            while (a.nextCheckpoint < checkpoints.size()
                    && pos.distanceTo(checkpoints.get(a.nextCheckpoint)) < checkpointRadius) {
                a.nextCheckpoint++;
            }
            double prog = a.nextCheckpoint - timePenalty * genTime;
            if (prog > a.maxProgress) a.maxProgress = prog;
        }
        if (alive == 0 || genTime > maxTime) nextGeneration();
    }

    private void nextGeneration() {
        population.sort(Comparator.comparingDouble((Agent a) -> a.maxProgress).reversed());
        double genBest = population.get(0).maxProgress;
        history.add(genBest);
        if (genBest > bestEver) {
            bestEver = genBest;
            bestEverGen = generation;
            bestBrainEver = population.get(0).brain.copy();
            int sc = bestBrainEver.in - 1;
            modelStatus.setText(String.format("Модель сохранена: пок. %d, чекпоинтов %.0f",
                    bestEverGen, bestEver));
            if (onBestUpdate != null) {
                onBestUpdate.accept(new AiModel(bestBrainEver, sc, sensorRange, sensorSpreadDeg));
            }
        }

        List<Agent> next = new ArrayList<>();
        int size = population.size();
        int elite = (int) Math.round(size * elitePercent / 100.0);
        elite = Math.max(0, Math.min(size, elite));

        for (int i = 0; i < elite; i++)
            next.add(new Agent(population.get(i).brain.copy()));

        while (next.size() < size) {
            Brain p1 = tournament();
            Brain p2 = tournament();
            Brain child = Brain.crossover(p1, p2, rnd);
            child.mutate(rnd, mutationRate, mutationPower);
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
        for (int i = 0; i < tournamentSize; i++) {
            Agent a = population.get(rnd.nextInt(population.size()));
            if (a.maxProgress > bestF) {
                bestF = a.maxProgress;
                best = a.brain;
            }
        }
        return best;
    }

    // ---------- Отрисовка ----------

    private void updateHud() {
        Agent best = population.stream().max(Comparator.comparingDouble(a -> a.maxProgress)).orElse(null);
        long alive = population.stream().filter(a -> a.alive).count();
        int goal = Math.max(1, checkpoints.size() - 1);
        hud.setText(String.format("Поколение %d   Живых %d/%d   Текущий %.1f / %d",
                generation, alive, population.size(),
                best == null ? 0 : best.maxProgress, goal));
        stats.setText(String.format(
                "Лучший за всё: %.1f  (пок. %d)\nТекущий:       %.1f\nЧекпоинтов:    %d",
                bestEver, bestEverGen,
                best == null ? 0 : best.maxProgress,
                goal));
    }

    private void draw() {
        if (canvas.getWidth() < 2 || canvas.getHeight() < 2) return;
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

            int sn = best.brain.in - 1;
            double[] sensors = Sensors.cast(track, new Vec2(best.car.getX(), best.car.getY()),
                    best.car.getAngle(), sn, sensorRange, Math.toRadians(sensorSpreadDeg));
            g.setStroke(Color.rgb(255, 255, 120, 0.7));
            g.setLineWidth(1);
            for (int i = 0; i < sn; i++) {
                double a = best.car.getAngle()
                        + (i - (sn - 1) / 2.0) * Math.toRadians(sensorSpreadDeg) / Math.max(1, sn - 1);
                double d = (1.0 - sensors[i]) * sensorRange;
                g.strokeLine(best.car.getX(), best.car.getY(),
                        best.car.getX() + Math.cos(a) * d,
                        best.car.getY() + Math.sin(a) * d);
            }
        }
    }

    private void drawGraph() {
        GraphicsContext g = graph.getGraphicsContext2D();
        double w = graph.getWidth(), h = graph.getHeight();
        g.setFill(Color.rgb(20, 22, 26));
        g.fillRect(0, 0, w, h);
        g.setStroke(Color.rgb(70, 72, 80));
        g.setLineWidth(1);
        g.strokeRect(0.5, 0.5, w - 1, h - 1);

        int goal = Math.max(1, checkpoints.size() - 1);
        double maxY = Math.max(goal, Math.max(bestEver, 1));

        double goalY = h - (goal / maxY) * (h - 14) - 7;
        g.setStroke(Color.rgb(90, 160, 90));
        g.setLineDashes(3, 3);
        g.strokeLine(0, goalY, w, goalY);

        double beY = h - (bestEver / maxY) * (h - 14) - 7;
        g.setStroke(Color.rgb(255, 215, 100));
        g.setLineDashes(5, 4);
        g.strokeLine(0, beY, w, beY);
        g.setLineDashes();

        if (history.size() >= 2) {
            g.setStroke(Color.ORANGE);
            g.setLineWidth(1.5);
            g.beginPath();
            for (int i = 0; i < history.size(); i++) {
                double x = (i / (double) (history.size() - 1)) * (w - 6) + 3;
                double y = h - (history.get(i) / maxY) * (h - 14) - 7;
                if (i == 0) g.moveTo(x, y);
                else g.lineTo(x, y);
            }
            g.stroke();
        }

        g.setFill(Color.rgb(200, 200, 200));
        g.setFont(Font.font(10));
        g.fillText("поколения →", 4, 12);
        g.fillText("0", 4, h - 3);
        g.fillText(String.format("%.0f", maxY), w - 30, 12);
    }

    public void stop() {
        if (timer != null) timer.stop();
    }
}