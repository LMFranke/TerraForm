package br.lucasfranke.model.item;

import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Item {

    private static final int ICON_CANVAS_SIZE = 64;
    private static final Font QUANTITY_FONT = new Font("SansSerif", Font.BOLD, 18);

    private TileType tileType;
    private int quantity;
    private int durability;
    private int maxDurability;

    private static final Map<String, BufferedImage> spriteCache = new HashMap<>();

    public Item() {
    }

    public Item(TileType tileType, int quantity) {
        this.tileType = tileType;
        this.maxDurability = (tileType != null && tileType.isTool())
                ? tileType.getToolTier().getMaxDurability()
                : 0;
        this.durability = this.maxDurability;
        this.quantity = (tileType != null && tileType.isTool()) ? 1 : quantity;
    }

    public Item(TileType tileType, int quantity, int durability) {
        this(tileType, quantity);
        if (this.maxDurability > 0) {
            this.durability = Math.clamp(durability, 1, this.maxDurability);
        }
    }

    public Item copyWithQuantity(int newQuantity) {
        return new Item(this.tileType, newQuantity, this.durability);
    }

    public BufferedImage getSprite() {
        if (tileType == null || tileType.getSprite() == null) {
            return null;
        }

        String cacheKey = tileType.name() + "_" + quantity;
        if (spriteCache.containsKey(cacheKey)) {
            return spriteCache.get(cacheKey);
        }

        BufferedImage originalSprite = tileType.getSprite();
        BufferedImage spriteCopy = new BufferedImage(
                ICON_CANVAS_SIZE,
                ICON_CANVAS_SIZE,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g = spriteCopy.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(originalSprite, 0, 0, ICON_CANVAS_SIZE, ICON_CANVAS_SIZE, null);

        if (!isTool()) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(QUANTITY_FONT);
            String text = String.valueOf(quantity);
            int textWidth = g.getFontMetrics().stringWidth(text);
            int textX = Math.max(2, ICON_CANVAS_SIZE - textWidth - 4);
            int textY = ICON_CANVAS_SIZE - 5;

            g.setColor(new Color(0, 0, 0, 220));
            g.drawString(text, textX + 2, textY + 2);
            g.drawString(text, textX + 1, textY + 1);

            g.setColor(Color.WHITE);
            g.drawString(text, textX, textY);
        }

        g.dispose();

        spriteCache.put(cacheKey, spriteCopy);
        return spriteCopy;
    }

    public boolean isTool() {
        return tileType != null && tileType.isTool();
    }

    public int getMaxStackSize() {
        return isTool() ? 1 : Player.MAX_STACK_SIZE;
    }

    public boolean damage(int amount) {
        if (!isTool() || maxDurability <= 0) {
            return false;
        }
        durability = Math.max(0, durability - amount);
        return durability == 0;
    }

    public int getDurability() {
        return durability;
    }

    public void setDurability(int durability) {
        this.durability = Math.clamp(durability, 0, maxDurability);
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public float getDurabilityRatio() {
        if (maxDurability <= 0) {
            return 1.0f;
        }
        return (float) durability / (float) maxDurability;
    }

    public TileType getTileType() {
        return tileType;
    }

    public void setTileType(TileType tileType) {
        this.tileType = tileType;
        this.maxDurability = (tileType != null && tileType.isTool())
                ? tileType.getToolTier().getMaxDurability()
                : 0;
        this.durability = this.maxDurability;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = isTool() ? Math.min(1, quantity) : quantity;
    }

    public void decreaseQuantity() {
        this.quantity--;
    }

    public void increaseQuantity() {
        if (!isTool()) {
            this.quantity++;
        }
    }

    public void increaseQuantity(int quantity) {
        if (!isTool()) {
            this.quantity += quantity;
        }
    }

    @Override
    public String toString() {
        return "Item{" +
                "tileType=" + tileType +
                ", quantity=" + quantity +
                ", durability=" + durability + "/" + maxDurability +
                '}';
    }
}