package br.lucasfranke.model.entity;

import br.lucasfranke.Assets;
import br.lucasfranke.Sprite;
import br.lucasfranke.model.type.MobType;
import br.lucasfranke.world.World;

public class CowMob extends Mob {

    public CowMob(double spawnWorldX, double spawnWorldY) {
        super(MobType.COW, spawnWorldX, spawnWorldY);
    }

    @Override
    protected void initMobSprites() {
        this.walkLeftSprite = new Sprite(Assets.cowWalkLeft, 16);
        this.walkRightSprite = new Sprite(Assets.cowWalkRight, 16);
        this.sprite = walkRightSprite;
    }

    @Override
    protected void updateAI(World world, Player player) {
        updatePassiveWanderAI(player);
    }
}