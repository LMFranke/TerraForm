package br.lucasfranke.world;

import java.util.Random;

public class Noise2D {
    private final int[] perm;

    public Noise2D(long seed) {
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

    private double grad(int hash, double x, double y) {
        int h = hash & 3;
        double u = h < 2 ? x : y;
        double v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    private double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    public double sample(double x, double y) {
        int xi = (int)Math.floor(x) & 255;
        int yi = (int)Math.floor(y) & 255;
        double xf = x - Math.floor(x);
        double yf = y - Math.floor(y);

        double u = fade(xf);
        double v = fade(yf);

        int aa = perm[xi] + yi;
        int ab = perm[xi] + yi + 1;
        int ba = perm[xi + 1] + yi;
        int bb = perm[xi + 1] + yi + 1;

        double x1 = lerp(u, grad(perm[aa], xf, yf), grad(perm[ba], xf - 1, yf));
        double x2 = lerp(u, grad(perm[ab], xf, yf - 1), grad(perm[bb], xf - 1, yf - 1));

        double res = lerp(v, x1, x2);
        return (res + 1) / 2.0; // normaliza 0..1
    }
}
