package com.example.roadracer.editor;

import com.example.roadracer.io.TrackStore;
import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;
import com.example.roadracer.render.TrackRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import java.io.File;

public class TrackEditorView extends BorderPane {
    private final Canvas canvas = new Canvas(1000, 650);
    private final Track track;
    private final Label status = new Label("Зажмите левую кнопку мыши и рисуйте трассу.");
    private boolean drawing;

    public TrackEditorView(Track track, Runnable onRace) {
        this.track = track;
        setStyle("-fx-background-color: #202328; -fx-padding: 10;");
        HBox toolbar = new HBox(8);
        toolbar.setStyle("-fx-padding: 8; -fx-alignment: center-left;");
        Button clear = new Button("Новая трасса");
        Button setStart = new Button("Старт здесь");
        Button save = new Button("Сохранить…");
        Button load = new Button("Загрузить…");
        Button race = new Button("Начать гонку ▶");
        Slider width = new Slider(40, 150, track.getRoadWidth());
        width.setPrefWidth(150); width.setShowTickLabels(true); width.setMajorTickUnit(50);
        toolbar.getChildren().addAll(clear, new Label("Ширина дороги:"), width, setStart, save, load, race);
        setTop(toolbar);
        canvas.widthProperty().bind(widthProperty().subtract(20));
        canvas.heightProperty().bind(heightProperty().subtract(100));
        canvas.widthProperty().addListener((o, a, b) -> render());
        canvas.heightProperty().addListener((o, a, b) -> render());
        width.valueProperty().addListener((o, a, b) -> { track.setRoadWidth(b.doubleValue()); render(); });
        canvas.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                drawing = true;
                Vec2 p = new Vec2(e.getX(), e.getY());
                track.getPoints().add(p);
                if (track.getPoints().size() == 1) track.setStartPosition(p);
                render();
            }
        });
        canvas.setOnMouseDragged(e -> {
            if (drawing) {
                Vec2 p = new Vec2(e.getX(), e.getY());
                var pts = track.getPoints();
                if (pts.isEmpty() || pts.get(pts.size() - 1).distanceTo(p) >= 4) pts.add(p);
                render();
            }
        });
        canvas.setOnMouseReleased(e -> drawing = false);
        clear.setOnAction(e -> { track.getPoints().clear(); track.setStartPosition(new Vec2(160, 160)); track.setStartAngle(0); render(); });
        setStart.setOnAction(e -> {
            if (track.getPoints().size() >= 2) {
                track.setStartPosition(track.getPoints().get(0));
                track.setStartAngle(track.angleAtStart());
                status.setText("Старт установлен в начале дороги."); render();
            } else status.setText("Сначала нарисуйте трассу хотя бы из двух точек.");
        });
        save.setOnAction(e -> saveTrack());
        load.setOnAction(e -> loadTrack(width));
        race.setOnAction(e -> {
            if (!track.isUsable()) { status.setText("Нарисуйте трассу перед началом гонки."); return; }
            if (track.getStartPosition() == null) track.setStartPosition(track.getPoints().get(0));
            onRace.run();
        });
        VBox bottom = new VBox(status);
        bottom.setStyle("-fx-padding: 8; -fx-text-fill: white;");
        status.setTextFill(Color.WHITE);
        setBottom(bottom);
        setCenter(canvas);
        render();
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        TrackRenderer.draw(g, track, canvas.getWidth(), canvas.getHeight());
        if (drawing) { g.setFill(Color.WHITE); g.fillOval(-3, -3, 6, 6); }
    }

    private void saveTrack() {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Сохранить трассу");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Трасса JSON", "*.json"));
        File file = chooser.showSaveDialog(getScene().getWindow());
        if (file != null) try { TrackStore.save(track, file.toPath()); status.setText("Трасса сохранена: " + file.getName()); }
        catch (Exception ex) { showError("Не удалось сохранить трассу", ex); }
    }

    private void loadTrack(Slider width) {
        FileChooser chooser = new FileChooser(); chooser.setTitle("Загрузить трассу");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Трасса JSON", "*.json"));
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) try {
            Track loaded = TrackStore.load(file.toPath());
            track.setName(loaded.getName()); track.setRoadWidth(loaded.getRoadWidth());
            track.setPoints(loaded.getPoints()); track.setStartPosition(loaded.getStartPosition()); track.setStartAngle(loaded.getStartAngle());
            width.setValue(track.getRoadWidth()); status.setText("Загружена трасса: " + file.getName()); render();
        } catch (Exception ex) { showError("Не удалось загрузить трассу", ex); }
    }

    private void showError(String title, Exception ex) {
        Alert alert = new Alert(Alert.AlertType.ERROR); alert.setTitle(title); alert.setHeaderText(title); alert.setContentText(ex.getMessage()); alert.showAndWait();
    }
}
