package com.example.roadracer.render;

import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public final class TrackRenderer {
    private TrackRenderer() {}

    public static void draw(GraphicsContext g, Track track, double width, double height) {
        g.setFill(Color.rgb(63, 135, 68));
        g.fillRect(0, 0, width, height);
        // Subtle grass pattern
        g.setStroke(Color.rgb(72, 145, 76, 0.35));
        g.setLineWidth(1);
        for (int y = 12; y < height; y += 28) for (int x = 10; x < width; x += 34) {
            g.strokeLine(x, y, x + 4, y + 2);
        }
        if (track.getPoints().size() >= 2) {
            g.setLineCap(StrokeLineCap.ROUND);
            g.setLineJoin(StrokeLineJoin.ROUND);
            g.setStroke(Color.rgb(34, 34, 38));
            g.setLineWidth(track.getRoadWidth() + 8);
            strokePoints(g, track);
            g.setStroke(Color.rgb(105, 108, 112));
            g.setLineWidth(track.getRoadWidth());
            strokePoints(g, track);
            g.setStroke(Color.rgb(230, 220, 160, 0.8));
            g.setLineWidth(2);
            g.setLineDashes(12, 12);
            strokePoints(g, track);
            g.setLineDashes();
        }
        drawStart(g, track);
    }

    private static void strokePoints(GraphicsContext g, Track track) {
        var points = track.getPoints();
        g.beginPath(); g.moveTo(points.get(0).x(), points.get(0).y());
        for (int i = 1; i < points.size(); i++) g.lineTo(points.get(i).x(), points.get(i).y());
        g.stroke();
    }

    public static void drawStart(GraphicsContext g, Track track) {
        Vec2 p = track.getStartPosition();
        double angle = track.getStartAngle();
        g.save(); g.translate(p.x(), p.y()); g.rotate(Math.toDegrees(angle));
        double w = track.getRoadWidth();
        int squares = 8;
        double square = w / squares;
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < squares; j++) {
                g.setFill((i + j) % 2 == 0 ? Color.WHITE : Color.BLACK);
                g.fillRect(-square + i * square, -w / 2 + j * square, square, square);
            }
        }
        g.restore();
    }
}
