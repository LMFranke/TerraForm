package br.lucasfranke.model.entity;

import br.lucasfranke.Assets;
import br.lucasfranke.Sprite;
import br.lucasfranke.model.type.MobType;
import br.lucasfranke.world.World;

public class ChickenMob extends Mob {

    public ChickenMob(double spawnWorldX, double spawnWorldY) {
        super(MobType.CHICKEN, spawnWorldX, spawnWorldY);
        this.gravity = 0.18;
        this.maxFallSpeed = 4.2;
    }

    @Override
    protected void initMobSprites() {
        this.walkLeftSprite = new Sprite(Assets.chickenWalkLeft, 10);
        this.walkRightSprite = new Sprite(Assets.chickenWalkRight, 10);
        this.sprite = walkRightSprite;
    }

    @Override
    protected void updateAI(World world, Player player) {
        updatePassiveWanderAI(player);
    }
}