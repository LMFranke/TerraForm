package br.lucasfranke.model.entity;

import br.lucasfranke.model.item.DroppedItemBundle;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.world.World;

import java.awt.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DroppedItem extends Entity {

    private static final int HITBOX_WIDTH = 22;
    private static final int HITBOX_HEIGHT = 16;
    private static final int DESPAWN_TIME_TICKS = 60 * 300; // 5 min in 60 UPS

    private final DroppedItemBundle bundle;
    private final float restingTilt = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.25f;

    private double vx;
    private int pickupDelay;
    private int ageTicks = 0;
    private boolean markedForRemoval = false;
    private float popScale = 1.0f;

    private DroppedItem(double worldX, double worldY, DroppedItemBundle bundle, double vx, double vy, int pickupDelay) {
        super();
        this.width = HITBOX_WIDTH;
        this.height = HITBOX_HEIGHT;
        this.worldX = worldX - width / 2.0;
        this.worldY = worldY - height / 2.0;
        this.bundle = bundle;
        this.vx = vx;
        this.fallSpeed = vy;
        this.isFalling = true;
        this.gravity = 0.34;
        this.maxFallSpeed = 11.5;
        this.pickupDelay = Math.max(0, pickupDelay);
    }

    public DroppedItem(double worldX, double worldY, Item initialItem, double vx, double vy, int pickupDelay) {
        this(worldX, worldY, new DroppedItemBundle(initialItem), vx, vy, pickupDelay);
    }

    public DroppedItem(double worldX, double worldY, List<Item> initialItems, double vx, double vy, int pickupDelay) {
        this(worldX, worldY, new DroppedItemBundle(initialItems), vx, vy, pickupDelay);
    }

    public void updatePhysics(World world, boolean magnetized) {
        if (isExpired()) {
            return;
        }

        ageTicks++;
        if (pickupDelay > 0) {
            pickupDelay--;
        }
        if (popScale > 1.0f) {
            popScale = Math.max(1.0f, popScale - 0.04f);
        }

        if (collidesAt(world, worldX, worldY)) {
            worldY -= 4.0;
            fallSpeed = 0;
            vx = 0;
            return;
        }

        if (!magnetized) {
            fallSpeed += gravity;
            if (fallSpeed > maxFallSpeed) {
                fallSpeed = maxFallSpeed;
            }
        }

        double previousVy = fallSpeed;
        moveWithCollision(world, vx, fallSpeed);

        if (hitHorizontalWall) {
            vx = -vx * 0.35;
        }

        if (!isFalling) {
            if (previousVy > 3.2 && !magnetized) {
                fallSpeed = -previousVy * 0.30;
                isFalling = true;
                vx *= 0.75;
            } else {
                vx *= 0.72;
                if (Math.abs(vx) < 0.05) {
                    vx = 0;
                }
            }
        } else if (!magnetized) {
            vx *= 0.96;
        }
    }

    public void applyMagnetVelocity(double targetX, double targetY, double dist, double maxRadius) {
        double safeDist = Math.max(1.0, dist);
        double pullStrength = (1.0 - (safeDist / maxRadius)) * 4.8 + 1.4;
        this.vx = ((targetX - getCenterX()) / safeDist) * pullStrength;
        this.fallSpeed = ((targetY - getCenterY()) / safeDist) * pullStrength;
    }

    public void absorbOther(DroppedItem other) {
        if (other == null || other == this || other.isExpired()) {
            return;
        }
        this.bundle.absorb(other.bundle);
        other.markForRemoval();
        this.ageTicks = Math.min(this.ageTicks, other.ageTicks);
        triggerPopAnimation();
    }

    public void triggerPopAnimation() {
        this.popScale = 1.28f;
    }

    public boolean canBePickedUp() {
        return pickupDelay <= 0 && !isExpired();
    }

    public boolean isExpired() {
        return markedForRemoval || bundle.isEmpty() || ageTicks >= DESPAWN_TIME_TICKS;
    }

    public void markForRemoval() {
        this.markedForRemoval = true;
    }

    public boolean isBlinkingBeforeDespawn() {
        return ageTicks > DESPAWN_TIME_TICKS - 600 && (ageTicks / 6) % 2 == 0;
    }

    public DroppedItemBundle getBundle() {
        return bundle;
    }

    public double getVx() {
        return vx;
    }

    public boolean isFallingState() {
        return isFalling;
    }

    public int getAgeTicks() {
        return ageTicks;
    }

    public float getPopScale() {
        return popScale;
    }

    public float getRestingTilt() {
        return restingTilt;
    }

    @Override
    public void draw(Graphics2D g) {}

}