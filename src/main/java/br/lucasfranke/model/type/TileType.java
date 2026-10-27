package br.lucasfranke.model.type;

import br.lucasfranke.Sprite;
import br.lucasfranke.sound.Sound;

import java.awt.image.BufferedImage;

public enum TileType {
    AIR(false, false, false, false, 0, 0),
    GRASS(true, true, true, false, 0, 55),
    DIRT(true, true, true, false, 0, 55),
    STONE(true, true, true, false, 0, 155),

    WOOD(false, true, true, false, 0, 75),
    LEAVES(false, true, true, false, 0, 20),
    PLANKS(true, true, true, false, 0, 65),

    COAL_ORE(true, true, true, false, 0, 170),
    IRON_ORE(true, true, true, false, 0, 210),
    GOLD_ORE(true, true, true, false, 0, 260),

    BACKGROUND_DIRT(false, false, false, false, 0, 45),
    BACKGROUND_GRASS(false, false, false, false, 0, 45),
    BACKGROUND_STONE(false, false, false, false, 0, 110),
    BACKGROUND_PLANKS(false, false, false, false, 0, 50),

    TORCH(false, true, true, false, 14, 1),
    CHEST(false, true, true, true, 0, 60),
    FURNACE(false, true, true, true, 10, 90),
    CRAFTING_TABLE(false, true, true, true, 0, 60),

    COAL(false, false, false, false, 0, 0),
    IRON_INGOT(false, false, false, false, 0, 0),
    GOLD_INGOT(false, false, false, false, 0, 0);

    private final boolean solid;
    private final boolean placeable;
    private final boolean breakable;
    private final boolean functional;
    private final int lightEmission;
    private final int hardness;
    private BufferedImage sprite;
    private Sprite animation;

    TileType(boolean solid, boolean placeable, boolean breakable, boolean functional, int lightEmission, int hardness) {
        this.solid = solid;
        this.placeable = placeable;
        this.breakable = breakable;
        this.functional = functional;
        this.lightEmission = lightEmission;
        this.hardness = hardness;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isPlaceable() {
        return placeable;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public boolean isFunctional() {
        return functional;
    }

    public int getLightEmission() {
        return lightEmission;
    }

    public int getHardness() {
        return hardness;
    }

    public TileType getDropType() {
        return switch (this) {
            case GRASS, BACKGROUND_GRASS -> GRASS;
            case DIRT, BACKGROUND_DIRT -> DIRT;
            case STONE, BACKGROUND_STONE -> STONE;
            case WOOD -> WOOD;
            case LEAVES -> LEAVES;
            case PLANKS, BACKGROUND_PLANKS -> PLANKS;
            case COAL_ORE -> COAL;
            case IRON_ORE -> IRON_ORE;
            case GOLD_ORE -> GOLD_ORE;
            case TORCH -> TORCH;
            case CHEST -> CHEST;
            case FURNACE -> FURNACE;
            case CRAFTING_TABLE -> CRAFTING_TABLE;
            default -> AIR;
        };
    }

    public TileType getBackgroundVariant() {
        return switch (this) {
            case GRASS -> BACKGROUND_GRASS;
            case DIRT -> BACKGROUND_DIRT;
            case STONE -> BACKGROUND_STONE;
            case PLANKS -> BACKGROUND_PLANKS;
            default -> AIR;
        };
    }

    public Sound getBreakSound() {
        return switch (this) {
            case DIRT, GRASS, WOOD, LEAVES, PLANKS,
                 BACKGROUND_DIRT, BACKGROUND_GRASS, BACKGROUND_PLANKS,
                 TORCH, CHEST, CRAFTING_TABLE
                    -> Sound.GRASS_BREAK;
            case STONE, COAL_ORE, IRON_ORE, GOLD_ORE, BACKGROUND_STONE, FURNACE
                    -> Sound.STONE_BREAK;
            default -> null;
        };
    }

    public BufferedImage getSprite() {
        if (animation != null) {
            return animation.getFrame();
        }
        return sprite;
    }

    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
    }

    public void setAnimation(BufferedImage[] frames, int frameDelay) {
        if (frames != null && frames.length > 0) {
            this.sprite = frames[0];
            this.animation = new Sprite(frames, frameDelay);
        }
    }

    public static void updateAnimations() {
        for (TileType type : values()) {
            if (type.animation != null) {
                type.animation.update();
            }
        }
    }
}