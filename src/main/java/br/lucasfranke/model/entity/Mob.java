package br.lucasfranke.model.entity;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.Sprite;
import br.lucasfranke.model.type.AnimationState;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.model.type.MobType;
import br.lucasfranke.world.World;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public abstract class Mob extends Entity {
    protected static final Random RANDOM = new Random();
    private static final Color HURT_OVERLAY_COLOR = new Color(255, 40, 40, 135);
    private static final Color BURN_OVERLAY_COLOR = new Color(255, 140, 20, 125);

    private static final BufferedImage TINT_BUFFER = new BufferedImage(
            EnginePanel.tileOriginalSquare,
            EnginePanel.tileOriginalSquare,
            BufferedImage.TYPE_INT_ARGB
    );

    protected final MobType mobType;
    protected Sprite walkLeftSprite;
    protected Sprite walkRightSprite;

    protected int aiTimer = 0;
    protected double moveIntentX = 0;
    protected int panicTicks = 0;
    protected int burnTickCounter = 0;

    protected Mob(MobType mobType, double spawnWorldX, double spawnWorldY) {
        super();
        this.mobType = mobType;
        this.worldX = spawnWorldX;
        this.worldY = spawnWorldY;
        this.width = mobType.getWidth();
        this.height = mobType.getHeight();
        this.maxHealth = mobType.getMaxHealth();
        this.health = this.maxHealth;
        this.gravity = 0.32;
        this.maxFallSpeed = 12.0;
        this.direction = RANDOM.nextBoolean() ? Direction.RIGHT : Direction.LEFT;
        initMobSprites();
    }

    protected abstract void initMobSprites();

    protected abstract void updateAI(World world, Player player);

    public void update(World world, Player player) {
        tickTimers();
        if (panicTicks > 0) {
            panicTicks--;
        }

        updateAI(world, player);

        double dx = moveIntentX + knockbackVx;
        if (Math.abs(moveIntentX) > 0.05) {
            direction = moveIntentX > 0 ? Direction.RIGHT : Direction.LEFT;
            state = AnimationState.WALK;
        } else {
            state = AnimationState.IDLE;
        }

        fallSpeed += gravity;
        if (fallSpeed > maxFallSpeed) {
            fallSpeed = maxFallSpeed;
        }

        moveWithCollision(world, dx, fallSpeed);

        if (hitHorizontalWall && !isFalling && Math.abs(moveIntentX) > 0.05) {
            fallSpeed = -6.2;
            isFalling = true;
        }

        Sprite currentSprite = (direction == Direction.LEFT) ? walkLeftSprite : walkRightSprite;
        if (currentSprite != null) {
            if (state == AnimationState.WALK) {
                currentSprite.update();
            } else {
                currentSprite.reset();
            }
            this.sprite = currentSprite;
        }
    }

    @Override
    public boolean takeDamage(int amount, double attackerCenterX, double knockbackStrength) {
        boolean damaged = super.takeDamage(amount, attackerCenterX, knockbackStrength);
        if (damaged && !mobType.isHostile()) {
            panicTicks = 150;
            double dir = (getCenterX() >= attackerCenterX) ? 1.0 : -1.0;
            moveIntentX = dir * (mobType.getBaseSpeed() * 1.65);
            aiTimer = 60;
        }
        return damaged;
    }

    protected void updatePassiveWanderAI(Player player) {
        if (panicTicks > 0) {
            if (aiTimer <= 0) {
                double dir = (getCenterX() >= player.getCenterX()) ? 1.0 : -1.0;
                moveIntentX = dir * (mobType.getBaseSpeed() * 1.5);
                aiTimer = 30 + RANDOM.nextInt(30);
            }
            aiTimer--;
            return;
        }

        if (aiTimer <= 0) {
            int roll = RANDOM.nextInt(100);
            if (roll < 45) {
                moveIntentX = 0;
            } else if (roll < 72) {
                moveIntentX = -mobType.getBaseSpeed();
            } else {
                moveIntentX = mobType.getBaseSpeed();
            }
            aiTimer = 60 + RANDOM.nextInt(120);
        } else {
            aiTimer--;
        }
    }

    public void draw(Graphics2D g2, Player player, Color shadowColor) {
        Sprite currentSprite = this.sprite;
        if (currentSprite == null) {
            return;
        }
        BufferedImage frame = currentSprite.getFrame();
        if (frame == null) {
            return;
        }

        int screenX = (int) Math.round(worldX - player.worldX + player.playerX);
        int screenY = (int) Math.round(worldY - player.worldY + player.playerY);

        if (screenX + width < -64 || screenX > EnginePanel.WIDTH + 64 || screenY + height < -64 || screenY > EnginePanel.HEIGHT + 64) {
            return;
        }

        boolean hasShadow = shadowColor != null && shadowColor.getAlpha() > 0;
        boolean hasHurt = hurtTimer > 0;
        boolean hasBurn = !hasHurt && burnTickCounter > 0;

        if (!hasShadow && !hasHurt && !hasBurn) {
            g2.drawImage(frame, screenX, screenY, width, height, null);
        } else {
            drawTintedSilhouette(g2, frame, screenX, screenY, shadowColor, hasShadow, hasHurt, hasBurn);
        }

        if (health < maxHealth) {
            drawHealthBar(g2, screenX, screenY);
        }
    }

    private void drawTintedSilhouette(Graphics2D g2, BufferedImage frame, int screenX, int screenY,
                                      Color shadowColor, boolean hasShadow, boolean hasHurt, boolean hasBurn) {
        int bufW = TINT_BUFFER.getWidth();
        int bufH = TINT_BUFFER.getHeight();

        Graphics2D bg = TINT_BUFFER.createGraphics();
        bg.setComposite(AlphaComposite.Clear);
        bg.fillRect(0, 0, bufW, bufH);

        bg.setComposite(AlphaComposite.SrcOver);
        bg.drawImage(frame, 0, 0, bufW, bufH, null);

        bg.setComposite(AlphaComposite.SrcAtop);
        if (hasShadow) {
            bg.setColor(shadowColor);
            bg.fillRect(0, 0, bufW, bufH);
        }
        if (hasHurt) {
            bg.setColor(HURT_OVERLAY_COLOR);
            bg.fillRect(0, 0, bufW, bufH);
        } else if (hasBurn) {
            bg.setColor(BURN_OVERLAY_COLOR);
            bg.fillRect(0, 0, bufW, bufH);
        }
        bg.dispose();

        g2.drawImage(TINT_BUFFER, screenX, screenY, width, height, null);
    }

    private void drawHealthBar(Graphics2D g2, int screenX, int screenY) {
        int barW = Math.max(32, width);
        int barH = 5;
        int barX = screenX + (width - barW) / 2;
        int barY = screenY - 10;

        float ratio = Math.clamp((float) health / maxHealth, 0f, 1f);
        g2.setColor(new Color(20, 20, 25, 220));
        g2.fillRoundRect(barX - 1, barY - 1, barW + 2, barH + 2, 4, 4);

        g2.setColor(mobType.isHostile() ? new Color(225, 55, 55) : new Color(75, 210, 95));
        g2.fillRoundRect(barX, barY, Math.round(barW * ratio), barH, 3, 3);
    }

    @Override
    public void draw(Graphics2D g) {
    }

    public MobType getMobType() {
        return mobType;
    }

    public int calculateDropAmount() {
        int min = mobType.getMinDrop();
        int max = mobType.getMaxDrop();
        return min + RANDOM.nextInt((max - min) + 1);
    }
}