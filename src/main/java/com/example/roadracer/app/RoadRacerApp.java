package com.example.roadracer.app;

import com.example.roadracer.ai.AiModel;
import com.example.roadracer.ai.AiRaceView;
import com.example.roadracer.editor.TrackEditorView;
import com.example.roadracer.game.RaceView;
import com.example.roadracer.game.VersusRaceView;
import com.example.roadracer.model.Track;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class RoadRacerApp extends Application {
    private Stage stage;
    private final Track track = new Track();
    private RaceView raceView;
    private AiRaceView aiView;
    private AiModel currentAiModel;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("RoadRacer — гонки и редактор трасс");
        showHome();
        stage.setMinWidth(900);
        stage.setMinHeight(650);
        stage.show();
    }

    private void showHome() {
        Label title = new Label("ROAD RACER");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label subtitle = new Label("2D-гонки с собственными трассами");
        subtitle.setStyle("-fx-text-fill: #c5cbd3; -fx-font-size: 15px;");
        VBox root = getVBox(title, subtitle);
        stage.setScene(new Scene(root, 1100, 720));
    }

    private VBox getVBox(Label title, Label subtitle) {
        Button editor = new Button("Редактор трасс");
        editor.setOnAction(e -> showEditor());
        Button race = new Button("Начать гонку");
        race.setOnAction(e -> {
            if (!track.isUsable()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Сначала создайте трассу в редакторе.");
                a.showAndWait();
            } else showRace();
        });

        Button ai = new Button("AI-тренировка");
        ai.setOnAction(e -> {
            if (!track.isUsable()) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Сначала создайте трассу в редакторе.");
                a.showAndWait();
            } else showAi();
        });

        Button versus = new Button("Гонка с ИИ");
        versus.setOnAction(e -> showVersus());
        VBox root = new VBox(18, title, subtitle, editor, race, versus, ai);
        root.setStyle("-fx-background-color: #202328; -fx-alignment: center; -fx-padding: 40;");
        return root;
    }

    private void showEditor() {
        TrackEditorView view = new TrackEditorView(track, this::showRace, this::showHome);
        stage.setScene(new Scene(view, 1100, 720));
    }

    private void showRace() {
        if (!track.isUsable()) return;
        if (raceView != null) raceView.stop();
        raceView = new RaceView(track, this::showEditor);
        stage.setScene(new Scene(raceView, 1100, 720));
        raceView.requestFocus();
    }


    private void showAi() {
        if (!track.isUsable()) return;
        if (raceView != null) raceView.stop();
        if (aiView != null) aiView.stop();
        aiView = new AiRaceView(track, this::showEditor, model -> this.currentAiModel = model);
        stage.setScene(new Scene(aiView, 1100, 720));
        aiView.requestFocus();
    }

    private void showVersus() {
        if (!track.isUsable()) {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Сначала создайте трассу.");
            a.showAndWait();
            return;
        }
        if (currentAiModel == null) {
            Alert a = new Alert(Alert.AlertType.INFORMATION,
                    "Сначала обучите ИИ хотя бы одно поколение (кнопка «AI-тренировка»).");
            a.showAndWait();
            return;
        }
        if (raceView != null) raceView.stop();
        if (aiView != null) aiView.stop();
        VersusRaceView view = new VersusRaceView(track, currentAiModel, this::showEditor);
        stage.setScene(new Scene(view, 1100, 720));
        view.requestFocus();
    }

    @Override
    public void stop() {
        if (raceView != null) raceView.stop();
        if (aiView != null) aiView.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
