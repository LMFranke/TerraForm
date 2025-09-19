package br.lucasfranke.model.type;

import java.awt.image.BufferedImage;

public enum TileType {
    AIR(false),
    GRASS(true),
    DIRT(true),
    STONE(true),

    BACKGROUND_DIRT(false),
    BACKGROUND_GRASS(false),
    BACKGROUND_STONE(false);

    private final boolean solid;
    private BufferedImage sprite;

    TileType(boolean solid) {
        this.solid = solid;
    }

    public boolean isSolid() {
        return solid;
    }

    public BufferedImage getSprite() {
        return sprite;
    }

    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
    }
}
