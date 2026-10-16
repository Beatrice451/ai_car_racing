package com.example.roadracer.game;

import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;

public class Car {
    private double x, y, angle, speed;
    private static final double MAX_SPEED = 310;
    private static final double REVERSE_SPEED = -100;
    private static final double ACCELERATION = 220;
    private static final double BRAKE = 360;
    private static final double FRICTION = 95;
    private static final double TURN_RATE = 2.7;

    public void reset(Track track) {
        Vec2 p = track.getStartPosition();
        x = p.x(); y = p.y(); angle = track.getStartAngle(); speed = 0;
    }

    public void update(double dt, boolean forward, boolean backward, boolean left, boolean right, Track track) {
        dt = Math.min(dt, 0.04);
        if (forward) speed += ACCELERATION * dt;
        if (backward) speed -= BRAKE * dt;
        if (!forward && !backward) {
            if (speed > 0) speed = Math.max(0, speed - FRICTION * dt);
            else speed = Math.min(0, speed + FRICTION * dt);
        }
        speed = Math.max(REVERSE_SPEED, Math.min(MAX_SPEED, speed));
        double steering = (left ? -1 : 0) + (right ? 1 : 0);
        double grip = Math.min(1, Math.abs(speed) / 55.0);
        angle += steering * TURN_RATE * grip * dt * (speed < 0 ? -1 : 1);

        double nextX = x + Math.cos(angle) * speed * dt;
        double nextY = y + Math.sin(angle) * speed * dt;
        if (track.distanceToRoad(new Vec2(nextX, nextY)) <= track.getRoadWidth() / 2.0 - 8) {
            x = nextX; y = nextY;
        } else {
            speed *= -0.18;
        }
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public double getSpeed() { return speed; }
}
