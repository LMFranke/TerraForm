package br.lucasfranke.model.type;

import br.lucasfranke.Sprite;
import java.awt.image.BufferedImage;

public enum TileType {

    AIR(false, false, false, 0, 0),
    GRASS(true, true, true, 0, 55),             // ~0.9 sec
    DIRT(true, true, true, 0, 55),              // ~0.9 sec
    STONE(true, true, true, 0, 150),            // ~2.5 sec
    BACKGROUND_DIRT(false, false, false, 0, 45),
    BACKGROUND_GRASS(false, false, false, 0, 45),
    BACKGROUND_STONE(false, false, false, 0, 110),
    TORCH(false, true, true, 14, 1);

    private final boolean solid;
    private final boolean placeable;
    private final boolean breakable;
    private final int lightEmission;
    private final int hardness;
    private BufferedImage sprite;
    private Sprite animation;

    TileType(boolean solid, boolean placeable, boolean breakable, int lightEmission, int hardness) {
        this.solid = solid;
        this.placeable = placeable;
        this.breakable = breakable;
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
            case TORCH -> TORCH;
            default -> AIR;
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