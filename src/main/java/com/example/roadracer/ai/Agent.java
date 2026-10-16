package com.example.roadracer.ai;

import com.example.roadracer.game.Car;

public class Agent {
    public final Car car = new Car();
    public final Brain brain;
    public double maxProgress;
    public boolean alive = true;
    public int nextCheckpoint = 1;

    public Agent(Brain b) { this.brain = b; }
}