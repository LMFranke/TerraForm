package br.lucasfranke.model.item;

import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Item {

    private TileType tileType;
    private int quantity;

    private static final Map<String, BufferedImage> spriteCache = new HashMap<>();
    private static final Font QUANTITY_FONT = new Font("SansSerif", Font.BOLD, 18);

    public Item() {

    }

    public Item(TileType tileType, int quantity) {
        this.tileType = tileType;
        this.quantity = quantity;
    }

    public BufferedImage getSprite() {
        String cacheKey = tileType.name() + "_" + quantity;

        if (spriteCache.containsKey(cacheKey)) {
            return spriteCache.get(cacheKey);
        }

        BufferedImage originalSprite = tileType.getSprite();
        BufferedImage spriteCopy = new BufferedImage(
                originalSprite.getWidth(),
                originalSprite.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        Graphics2D g = spriteCopy.createGraphics();
        g.drawImage(originalSprite, 0, 0, null);

        g.setFont(QUANTITY_FONT);
        g.setColor(Color.WHITE);
        String text = "" + quantity;
        int textWidth = g.getFontMetrics().stringWidth(text);
        g.drawString(text, spriteCopy.getWidth() - textWidth - 4, spriteCopy.getHeight() - 5);

        g.dispose();

        spriteCache.put(cacheKey, spriteCopy);

        return spriteCopy;
    }

    public TileType getTileType() {
        return tileType;
    }

    public void setTileType(TileType tileType) {
        this.tileType = tileType;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "Item{" +
                "tileType=" + tileType +
                ", quantity=" + quantity +
                '}';
    }
}
