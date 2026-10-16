package com.example.roadracer.model;

public record Vec2(double x, double y) {
    public double distanceTo(Vec2 other) {
        return Math.hypot(x - other.x, y - other.y);
    }

    public Vec2 add(Vec2 other) { return new Vec2(x + other.x, y + other.y); }
    public Vec2 subtract(Vec2 other) { return new Vec2(x - other.x, y - other.y); }
    public Vec2 multiply(double scalar) { return new Vec2(x * scalar, y * scalar); }
}
