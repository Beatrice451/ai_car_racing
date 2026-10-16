package com.example.roadracer.model;

import java.util.ArrayList;
import java.util.List;

public class Track {
    private String name = "My track";
    private double roadWidth = 90;
    private List<Vec2> points = new ArrayList<>();
    private Vec2 startPosition = new Vec2(160, 160);
    private double startAngle = 0;

    public Track() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getRoadWidth() { return roadWidth; }
    public void setRoadWidth(double roadWidth) { this.roadWidth = Math.max(30, Math.min(180, roadWidth)); }
    public List<Vec2> getPoints() { return points; }
    public void setPoints(List<Vec2> points) { this.points = points == null ? new ArrayList<>() : points; }
    public Vec2 getStartPosition() { return startPosition; }
    public void setStartPosition(Vec2 startPosition) { this.startPosition = startPosition; }
    public double getStartAngle() { return startAngle; }
    public void setStartAngle(double startAngle) { this.startAngle = startAngle; }

    public boolean isUsable() { return points.size() >= 2; }

    public double distanceToRoad(Vec2 p) {
        if (points.size() < 2) return Double.POSITIVE_INFINITY;
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i < points.size(); i++) {
            Vec2 a = points.get(i - 1), b = points.get(i);
            double dx = b.x() - a.x(), dy = b.y() - a.y();
            double len2 = dx * dx + dy * dy;
            double t = len2 == 0 ? 0 : ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / len2;
            t = Math.max(0, Math.min(1, t));
            double px = a.x() + t * dx, py = a.y() + t * dy;
            best = Math.min(best, Math.hypot(p.x() - px, p.y() - py));
        }
        return best;
    }

    public double angleAtStart() {
        if (points.size() < 2) return startAngle;
        Vec2 a = points.get(0), b = points.get(1);
        return Math.atan2(b.y() - a.y(), b.x() - a.x());
    }

    public double progress(Vec2 p) {
    if (points.size() < 2) return 0;
    double best = Double.POSITIVE_INFINITY;
    double bestIdx = 0;
    for (int i = 1; i < points.size(); i++) {
        Vec2 a = points.get(i - 1), b = points.get(i);
        double dx = b.x() - a.x(), dy = b.y() - a.y();
        double len2 = dx * dx + dy * dy;
        double t = len2 == 0 ? 0 : ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / len2;
        t = Math.max(0, Math.min(1, t));
        double px = a.x() + t * dx, py = a.y() + t * dy;
        double d = Math.hypot(p.x() - px, p.y() - py);
        if (d < best) { best = d; bestIdx = (i - 1) + t; }
    }
    return bestIdx;
}
}
