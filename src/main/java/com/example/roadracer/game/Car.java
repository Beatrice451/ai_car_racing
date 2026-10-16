package com.example.roadracer.game;

import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;

public class Car {
    private double x, y, angle, speed;
    private static final double MAX_SPEED = 310;
    private static final double REVERSE_SPEED = -100;
    private static final double ACCELERATION = 500;
    private static final double BRAKE = 360;
    private static final double FRICTION = 95;
    private static final double TURN_RATE = 3.5;
    private boolean crashed;

    public void reset(Track track) {
        Vec2 p = track.getStartPosition();
        x = p.x();
        y = p.y();
        angle = track.getStartAngle();
        speed = 0;
    }

    public void update(double dt, boolean forward, boolean backward, boolean left, boolean right, Track track) {
        double throttle = (forward ? 1 : 0) - (backward ? 1 : 0);
        double steer = (left ? -1 : 0) + (right ? 1 : 0);
        updateAi(dt, steer, throttle, track);
    }

    public void updateAi(double dt, double steer, double throttle, Track track) {
        dt = Math.min(dt, 0.04);
        if (throttle > 0) speed += ACCELERATION * throttle * dt;
        else if (throttle < 0) speed += BRAKE * throttle * dt;
        else {
            if (speed > 0) speed = Math.max(0, speed - FRICTION * dt);
            else speed = Math.min(0, speed + FRICTION * dt);
        }
        speed = Math.max(REVERSE_SPEED, Math.min(MAX_SPEED, speed));
        double grip = Math.min(1, Math.abs(speed) / 55.0);
        angle += steer * TURN_RATE * grip * dt * (speed < 0 ? -1 : 1);

        double nextX = x + Math.cos(angle) * speed * dt;
        double nextY = y + Math.sin(angle) * speed * dt;
        if (track.distanceToRoad(new Vec2(nextX, nextY)) <= track.getRoadWidth() / 2.0 - 8) {
            x = nextX;
            y = nextY;
        } else {
            crashed = true;
            speed *= -0.18;
        }
    }

    public boolean isCrashed() {
        return crashed;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getAngle() {
        return angle;
    }

    public double getSpeed() {
        return speed;
    }
}
