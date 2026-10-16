package com.example.roadracer.ai;

import com.example.roadracer.model.Track;
import com.example.roadracer.model.Vec2;

public final class Sensors {
    private Sensors() {
    }

    public static double[] cast(Track track, Vec2 pos, double angle,
                                int count, double maxRange, double spreadRad) {
        double[] out = new double[count];
        double half = track.getRoadWidth() / 2.0 - 4;
        double step = 4;
        for (int i = 0; i < count; i++) {
            double a = angle + (i - (count - 1) / 2.0) * spreadRad / Math.max(1, count - 1);
            double dx = Math.cos(a), dy = Math.sin(a);
            double dist = maxRange;
            for (double d = 4; d <= maxRange; d += step) {
                double px = pos.x() + dx * d, py = pos.y() + dy * d;
                if (track.distanceToRoad(new Vec2(px, py)) > half) {
                    dist = d;
                    break;
                }
            }
            out[i] = 1.0 - dist / maxRange; // 1 = стена рядом, 0 = далеко
        }
        return out;
    }
}