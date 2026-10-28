package br.lucasfranke;

import java.awt.image.BufferedImage;

public class Sprite {

    private final BufferedImage[] frames;
    private volatile int currentFrame;
    private final int frameDelay;
    private int counter;

    public Sprite(BufferedImage[] frames, int frameDelay) {
        this.frames = frames;
        this.frameDelay = frameDelay;
        this.currentFrame = 0;
        this.counter = 0;
    }

    public void update() {
        counter++;
        if (counter >= frameDelay) {
            counter = 0;
            currentFrame = (currentFrame + 1) % frames.length;
        }
    }

    public BufferedImage getFrame() {
        int index = currentFrame;
        if (index < 0 || index >= frames.length) {
            return frames[0];
        }
        return frames[index];
    }

    public void reset() {
        currentFrame = 0;
        counter = 0;
    }
}