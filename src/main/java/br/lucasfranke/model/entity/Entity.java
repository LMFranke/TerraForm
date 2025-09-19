package br.lucasfranke.model.entity;

import br.lucasfranke.model.type.AnimationState;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.Sprite;

import java.awt.*;

public abstract class Entity {

    public Sprite sprite;
    public AnimationState state;

    protected double fallSpeed = 0;
    protected boolean isFalling = false;
    protected double gravity = 0.5;
    protected double maxFallSpeed = 15;

    public int worldX;
    public int worldY;

    public Rectangle bounds;
    protected int width, height;
    public boolean isCollision;
    public int speed;
    protected Direction direction;

    public Entity() {
        state = AnimationState.IDLE;
    }

    public void update() {}

    public abstract void draw(Graphics2D g);
}
