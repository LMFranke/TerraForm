package br.lucasfranke.model.entity;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.Sprite;
import br.lucasfranke.model.type.AnimationState;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.world.World;

import java.awt.*;

public abstract class Entity {
    public Sprite sprite;
    public AnimationState state;

    protected double fallSpeed = 0;
    protected boolean isFalling = false;
    protected double gravity = 0.5;
    protected double maxFallSpeed = 15;

    public double worldX;
    public double worldY;
    public int width;
    public int height;
    public int speed;
    protected Direction direction = Direction.RIGHT;

    public int maxHealth = 20;
    public int health = 20;
    public int hurtTimer = 0;
    protected double knockbackVx = 0;
    protected boolean hitHorizontalWall = false;

    public Entity() {
        state = AnimationState.IDLE;
    }

    public void tickTimers() {
        if (hurtTimer > 0) {
            hurtTimer--;
        }
        if (Math.abs(knockbackVx) > 0.08) {
            knockbackVx *= 0.82;
        } else {
            knockbackVx = 0;
        }
    }

    public boolean takeDamage(int amount, double attackerCenterX, double knockbackStrength) {
        if (hurtTimer > 0 || amount <= 0 || isDead()) {
            return false;
        }
        health = Math.max(0, health - amount);
        hurtTimer = 22;
        if (knockbackStrength > 0) {
            double dir = (getCenterX() >= attackerCenterX) ? 1.0 : -1.0;
            knockbackVx = dir * knockbackStrength;
            fallSpeed = -4.2;
            isFalling = true;
        }
        return true;
    }

    public void heal(int amount) {
        if (amount > 0 && !isDead()) {
            health = Math.min(maxHealth, health + amount);
        }
    }

    public boolean isDead() {
        return health <= 0;
    }

    public double getCenterX() {
        return worldX + width / 2.0;
    }

    public double getCenterY() {
        return worldY + height / 2.0;
    }

    public Direction getDirection() {
        return direction;
    }

    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(worldX), (int) Math.round(worldY), width, height);
    }

    public void moveWithCollision(World world, double dx, double dy) {
        hitHorizontalWall = false;
        if (dx != 0) {
            if (!collidesAt(world, worldX + dx, worldY)) {
                worldX += dx;
            } else {
                hitHorizontalWall = true;
            }
        }

        isFalling = true;
        if (dy != 0 && !collidesAt(world, worldX, worldY + dy)) {
            worldY += dy;
        } else {
            if (dy > 0) {
                isFalling = false;
                worldY = Math.floor((worldY + height + dy) / EnginePanel.tileSquare) * EnginePanel.tileSquare - height;
                fallSpeed = 0;
            } else if (dy < 0) {
                fallSpeed = 0;
            }
        }
    }

    public boolean collidesAt(World world, double testX, double testY) {
        int tileSize = EnginePanel.tileSquare;
        int leftTile = (int) Math.floor(testX / tileSize);
        int rightTile = (int) Math.floor((testX + width - 1) / tileSize);
        int topTile = (int) Math.floor(testY / tileSize);
        int bottomTile = (int) Math.floor((testY + height - 1) / tileSize);

        for (int ty = topTile; ty <= bottomTile; ty++) {
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (world.isSolid(tx, ty)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void update() {}

    public abstract void draw(Graphics2D g);
}