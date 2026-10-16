package com.example.roadracer.ai;

import java.util.Random;

public class Brain {
    public final int in, hid, out;
    public final double[] p;
    private final int w1, b1, w2, b2;

    public Brain(int in, int hid, int out) {
        this.in = in;
        this.hid = hid;
        this.out = out;
        int n1 = hid * in, n2 = out * hid;
        w1 = 0;
        b1 = n1;
        w2 = n1 + hid;
        b2 = n1 + hid + n2;
        p = new double[n1 + hid + n2 + out];
    }

    public void randomize(Random rnd, double scale) {
        for (int i = 0; i < p.length; i++) p[i] = rnd.nextGaussian() * scale;
    }

    public double[] forward(double[] x) {
        double[] h = new double[hid];
        for (int i = 0; i < hid; i++) {
            double s = p[b1 + i];
            for (int j = 0; j < in; j++) s += p[w1 + i * in + j] * x[j];
            h[i] = Math.tanh(s);
        }
        double[] y = new double[out];
        for (int i = 0; i < out; i++) {
            double s = p[b2 + i];
            for (int j = 0; j < hid; j++) s += p[w2 + i * hid + j] * h[j];
            y[i] = Math.tanh(s);
        }
        return y;
    }

    public Brain copy() {
        Brain c = new Brain(in, hid, out);
        System.arraycopy(p, 0, c.p, 0, p.length);
        return c;
    }

    public static Brain crossover(Brain a, Brain b, Random rnd) {
        Brain c = new Brain(a.in, a.hid, a.out);
        for (int i = 0; i < c.p.length; i++) c.p[i] = rnd.nextBoolean() ? a.p[i] : b.p[i];
        return c;
    }

    public void mutate(Random rnd, double rate, double power) {
        for (int i = 0; i < p.length; i++)
            if (rnd.nextDouble() < rate) p[i] += rnd.nextGaussian() * power;
    }
}