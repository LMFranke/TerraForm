package br.lucasfranke.model.type;

import br.lucasfranke.Sprite;
import java.awt.image.BufferedImage;

public enum TileType {

    AIR(false, false, false, 0),
    GRASS(true, true, true, 0),
    DIRT(true, true, true, 0),
    STONE(true, true, true, 0),
    BACKGROUND_DIRT(false, false, false, 0),
    BACKGROUND_GRASS(false, false, false, 0),
    BACKGROUND_STONE(false, false, false, 0),
    TORCH(false, true, true, 14);

    private final boolean solid;
    private final boolean placeable;
    private final boolean breakable;
    private final int lightEmission;
    private BufferedImage sprite;
    private Sprite animation;

    TileType(boolean solid, boolean placeable, boolean breakable, int lightEmission) {
        this.solid = solid;
        this.placeable = placeable;
        this.breakable = breakable;
        this.lightEmission = lightEmission;
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