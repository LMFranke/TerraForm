package br.lucasfranke.world;

import java.util.Random;

public class Noise1D {

    private final int[] perm;

    public Noise1D(long seed) {
        perm = new int[512];
        Random rand = new Random(seed);

        int[] p = new int[256];
        for (int i = 0; i < 256; i++) p[i] = i;

        for (int i = 255; i > 0; i--) {
            int j = rand.nextInt(i + 1);
            int tmp = p[i];
            p[i] = p[j];
            p[j] = tmp;
        }

        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    private double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private double grad(int hash, double x) {
        return ((hash & 1) == 0 ? x : -x);
    }

    public double sample(double x) {
        int xi = (int)Math.floor(x) & 255;
        double xf = x - Math.floor(x);
        double u = fade(xf);

        int a = perm[xi];
        int b = perm[xi + 1];

        double res = lerp(u, grad(a, xf), grad(b, xf - 1));
        return (res + 1) / 2.0; // normaliza 0..1
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}

