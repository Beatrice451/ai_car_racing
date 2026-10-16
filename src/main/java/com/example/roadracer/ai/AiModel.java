package com.example.roadracer.ai;

public class AiModel {
    public final Brain brain;
    public final int sensorCount;
    public final double sensorRange;
    public final double sensorSpreadDeg;

    public AiModel(Brain brain, int sensorCount, double sensorRange, double sensorSpreadDeg) {
        this.brain = brain;
        this.sensorCount = sensorCount;
        this.sensorRange = sensorRange;
        this.sensorSpreadDeg = sensorSpreadDeg;
    }
}