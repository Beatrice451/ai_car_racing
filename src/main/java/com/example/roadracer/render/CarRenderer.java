package com.example.roadracer.render;

import com.example.roadracer.game.Car;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public final class CarRenderer {
    private CarRenderer() {}
    public static void draw(GraphicsContext g, Car car) {
        g.save(); g.translate(car.getX(), car.getY()); g.rotate(Math.toDegrees(car.getAngle()));
        g.setFill(Color.rgb(0, 0, 0, 0.3)); g.fillRoundRect(-18, -9, 42, 22, 8, 8);
        g.setFill(Color.DARKSLATEGRAY);
        g.fillRoundRect(-17, -12, 36, 24, 8, 8);
        g.setFill(Color.CRIMSON); g.fillRoundRect(-19, -10, 36, 20, 7, 7);
        g.setFill(Color.rgb(160, 220, 240)); g.fillRoundRect(-5, -8, 12, 16, 4, 4);
        g.setFill(Color.rgb(255, 245, 190)); g.fillRect(13, -7, 3, 4); g.fillRect(13, 3, 3, 4);
        g.setFill(Color.rgb(120, 20, 20)); g.fillRect(-18, -7, 3, 4); g.fillRect(-18, 3, 3, 4);
        g.restore();
    }
}
