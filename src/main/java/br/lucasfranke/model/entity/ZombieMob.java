package br.lucasfranke.model.entity;

import br.lucasfranke.Assets;
import br.lucasfranke.EnginePanel;
import br.lucasfranke.Sprite;
import br.lucasfranke.model.type.MobType;
import br.lucasfranke.world.World;

public class ZombieMob extends Mob {
    private static final double AGGRO_RANGE_PIXELS = EnginePanel.tileSquare * 14.0;

    public ZombieMob(double spawnWorldX, double spawnWorldY) {
        super(MobType.ZOMBIE, spawnWorldX, spawnWorldY);
    }

    @Override
    protected void initMobSprites() {
        this.walkLeftSprite = new Sprite(Assets.zombieWalkLeft, 14);
        this.walkRightSprite = new Sprite(Assets.zombieWalkRight, 14);
        this.sprite = walkRightSprite;
    }

    @Override
    protected void updateAI(World world, Player player) {
        checkSunlightBurn(world);

        double dx = player.getCenterX() - this.getCenterX();
        double dy = player.getCenterY() - this.getCenterY();
        double distSq = dx * dx + dy * dy;

        if (distSq <= AGGRO_RANGE_PIXELS * AGGRO_RANGE_PIXELS) {
            if (Math.abs(dx) > 6) {
                moveIntentX = Math.signum(dx) * mobType.getBaseSpeed();
            } else {
                moveIntentX = 0;
            }
        } else {
            updatePassiveWanderAI(player);
        }
    }

    private void checkSunlightBurn(World world) {
        if (world.getSunLightLevel() < 13) {
            burnTickCounter = 0;
            return;
        }
        int tileX = (int) Math.floor(getCenterX() / EnginePanel.tileSquare);
        int tileY = (int) Math.floor(getCenterY() / EnginePanel.tileSquare);
        if (world.getStaticLight(tileX, tileY) >= 14) {
            burnTickCounter++;
            if (burnTickCounter >= 60) {
                burnTickCounter = 0;
                takeDamage(2, getCenterX(), 0.0);
            }
        } else {
            burnTickCounter = 0;
        }
    }
}