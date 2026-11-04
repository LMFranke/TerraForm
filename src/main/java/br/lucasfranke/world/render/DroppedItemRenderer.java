package br.lucasfranke.world.render;

import br.lucasfranke.graphics.Assets;
import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.entity.DroppedItem;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.DroppedItemBundle;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;

public class DroppedItemRenderer {

    private static final Font BADGE_FONT = new Font("SansSerif", Font.BOLD, 10);

    private final BufferedImage shadeBuffer = new BufferedImage(
            EnginePanel.tileOriginalSquare,
            EnginePanel.tileOriginalSquare,
            BufferedImage.TYPE_INT_ARGB
    );

    public void draw(Graphics2D g2, DroppedItem drop, Player player, Color shadowColor) {
        if (drop.isExpired() || drop.isBlinkingBeforeDespawn()) {
            return;
        }

        int screenX = (int) Math.round(drop.worldX - player.worldX + player.playerX);
        int screenY = (int) Math.round(drop.worldY - player.worldY + player.playerY);

        if (screenX + 48 < 0 || screenX - 48 > EnginePanel.WIDTH
                || screenY + 48 < 0 || screenY - 48 > EnginePanel.HEIGHT) {
            return;
        }

        DroppedItemBundle bundle = drop.getBundle();
        int centerX = screenX + drop.width / 2;
        int groundBaseY = screenY + drop.height;

        drawContactShadow(g2, centerX, groundBaseY, bundle.isLootBag(), drop.isFallingState());

        if (bundle.isLootBag()) {
            drawLootBag(g2, drop, bundle, centerX, groundBaseY, shadowColor);
        } else {
            Item primary = bundle.getPrimaryItem();
            if (primary != null) {
                drawSingleItem(g2, drop, primary, centerX, groundBaseY, shadowColor);
            }
        }
    }

    private void drawContactShadow(Graphics2D g2, int centerX, int groundBaseY, boolean isLootBag, boolean isFalling) {
        int shadowW = isLootBag ? 24 : 20;
        int shadowH = 6;
        g2.setColor(new Color(0, 0, 0, isFalling ? 55 : 105));
        g2.fillOval(centerX - shadowW / 2, groundBaseY - shadowH / 2 - 1, shadowW, shadowH);
    }

    private void drawLootBag(Graphics2D g2, DroppedItem drop, DroppedItemBundle bundle,
                             int centerX, int groundBaseY, Color shadowColor) {
        BufferedImage bagSprite = Assets.lootBagSprite != null
                ? Assets.lootBagSprite
                : TileType.CHEST.getSprite();
        if (bagSprite == null) {
            return;
        }

        float popScale = drop.getPopScale();
        int baseSize = 28;
        int drawW = Math.round(baseSize * popScale);
        int drawH = Math.round(baseSize * (2.0f - popScale * 0.85f));
        int drawX = centerX - drawW / 2;
        int drawY = groundBaseY - drawH + 2;

        BufferedImage shaded = getShadedFrame(bagSprite, shadowColor);
        g2.drawImage(shaded, drawX, drawY, drawW, drawH, null);

        drawQuantityBadge(g2, bundle.getTotalQuantity(), centerX + 2, drawY + drawH - 12);
    }

    private void drawQuantityBadge(Graphics2D g2, int totalQty, int bx, int by) {
        String badgeText = totalQty > 99 ? "99+" : String.valueOf(totalQty);
        g2.setFont(BADGE_FONT);
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(badgeText) + 8;
        int bh = 13;

        g2.setColor(new Color(22, 24, 30, 225));
        g2.fillRoundRect(bx, by, bw, bh, 6, 6);
        g2.setColor(new Color(235, 195, 65, 220));
        g2.drawRoundRect(bx, by, bw, bh, 6, 6);
        g2.setColor(new Color(255, 240, 180));
        g2.drawString(badgeText, bx + 4, by + 10);
    }

    private void drawSingleItem(Graphics2D g2, DroppedItem drop, Item item,
                                int centerX, int groundBaseY, Color shadowColor) {
        TileType type = item.getTileType();
        BufferedImage rawSprite = type.getSprite();
        if (rawSprite == null) {
            return;
        }

        BufferedImage shaded = getShadedFrame(rawSprite, shadowColor);
        float popScale = drop.getPopScale();
        float restingTilt = drop.getRestingTilt();
        boolean isFalling = drop.isFallingState();
        double vx = drop.getVx();

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

        if (isFlatRestingType(item, type)) {
            int size = 26;
            double angle = isFalling
                    ? (drop.getAgeTicks() * 0.18 * (vx >= 0 ? 1 : -1))
                    : Math.toRadians(54) + restingTilt;

            g.translate(centerX, groundBaseY - 5);
            g.scale(popScale, 0.78 * popScale);
            g.rotate(angle);
            g.drawImage(shaded, -size / 2, -size / 2, size, size, null);
        } else if (type.isSolid() || type.isFunctional()) {
            int drawSize = Math.round(20 * popScale);
            double angle = isFalling ? (vx * 0.08) : restingTilt;

            g.translate(centerX, groundBaseY - drawSize / 2 - 1);
            g.rotate(angle);

            if (item.getQuantity() >= 16) {
                g.drawImage(shaded, -drawSize / 2 - 4, -drawSize / 2 - 2, drawSize, drawSize, null);
            }
            if (item.getQuantity() >= 3) {
                g.drawImage(shaded, -drawSize / 2 + 3, -drawSize / 2 - 1, drawSize, drawSize, null);
            }

            g.setColor(new Color(15, 15, 20, 140));
            g.fillRoundRect(-drawSize / 2 - 1, -drawSize / 2 - 1, drawSize + 2, drawSize + 2, 4, 4);
            g.drawImage(shaded, -drawSize / 2, -drawSize / 2, drawSize, drawSize, null);
        } else {
            int drawW = Math.round(22 * popScale);
            int drawH = Math.round(22 * 0.85f * popScale);
            double angle = isFalling ? (vx * 0.1) : restingTilt * 1.5;

            g.translate(centerX, groundBaseY - drawH / 2 - 1);
            g.rotate(angle);
            if (item.getQuantity() >= 4) {
                g.drawImage(shaded, -drawW / 2 + 3, -drawH / 2 - 2, drawW, drawH, null);
            }
            g.drawImage(shaded, -drawW / 2, -drawH / 2, drawW, drawH, null);
        }

        g.dispose();
    }

    private boolean isFlatRestingType(Item item, TileType type) {
        return item.isTool()
                || type == TileType.STICK
                || type == TileType.TORCH
                || type == TileType.OAK_SAPLING;
    }

    private BufferedImage getShadedFrame(BufferedImage sprite, Color shadowColor) {
        if (shadowColor == null || shadowColor.getAlpha() <= 0) {
            return sprite;
        }
        int bufW = shadeBuffer.getWidth();
        int bufH = shadeBuffer.getHeight();
        Graphics2D bg = shadeBuffer.createGraphics();
        bg.setComposite(AlphaComposite.Clear);
        bg.fillRect(0, 0, bufW, bufH);
        bg.setComposite(AlphaComposite.SrcOver);
        bg.drawImage(sprite, 0, 0, bufW, bufH, null);
        bg.setComposite(AlphaComposite.SrcAtop);
        bg.setColor(shadowColor);
        bg.fillRect(0, 0, bufW, bufH);
        bg.dispose();
        return shadeBuffer;
    }
}