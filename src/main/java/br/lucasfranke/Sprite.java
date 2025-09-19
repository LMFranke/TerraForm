package br.lucasfranke;

import java.awt.image.BufferedImage;

public class Sprite {
    private BufferedImage[] frames;
    private int currentFrame;
    private int frameDelay;
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
            currentFrame++;
            if (currentFrame >= frames.length) {
                currentFrame = 0;
            }
        }
    }

    public BufferedImage getFrame() {
        return frames[currentFrame];
    }

    public void reset() {
        currentFrame = 0;
        counter = 0;
    }
}

