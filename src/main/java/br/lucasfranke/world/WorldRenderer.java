package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;

public class WorldRenderer {

    private static final Color SKY_COLOR = new Color(75, 145, 235);
    private static final Color MOUSE_HOVER_FOREGROUND_COLOR = new Color(255, 255, 0, 120);
    private static final Color MOUSE_HOVER_BACKGROUND_COLOR = new Color(0, 200, 255, 130);
    private static final Color[] SHADOW_COLORS = new Color[16];

    private static final double BASE_PLAYER_LIGHT_RADIUS = 2.2;
    private static final int BASE_PLAYER_LIGHT_INTENSITY = 8;
    private static final double TORCH_HAND_LIGHT_RADIUS = 6.0;
    private static final int TORCH_HAND_LIGHT_INTENSITY = 14;

    private Integer cachedTorchBottomOffset = null;

    static {
        for (int i = 0; i < 16; i++) {
            float opacity = 1.0f - (i / 15.0f);
            SHADOW_COLORS[i] = new Color(0, 0, 0, opacity);
        }
    }

    public void draw(Graphics2D g2, World world, Player player, BlockInteractionController interaction) {
        int tileSize = EnginePanel.tileSquare;
        int playerWorldX = (int) player.worldX;
        int playerWorldY = (int) player.worldY;
        int playerScreenX = player.playerX;
        int playerScreenY = player.playerY;

        int playerTileX = (playerWorldX + player.width / 2) / tileSize;
        int playerTileY = (playerWorldY + player.height / 2) / tileSize;

        boolean holdingTorch = isHoldingTorch(player);
        double activeLightRadius = holdingTorch ? TORCH_HAND_LIGHT_RADIUS : BASE_PLAYER_LIGHT_RADIUS;
        int activeLightIntensity = holdingTorch ? TORCH_HAND_LIGHT_INTENSITY : BASE_PLAYER_LIGHT_INTENSITY;

        int worldStartX = playerWorldX - playerScreenX;
        int worldStartY = playerWorldY - playerScreenY;
        int startCol = Math.floorDiv(worldStartX, tileSize);
        int endCol = Math.floorDiv(worldStartX + EnginePanel.WIDTH, tileSize);
        int startRow = Math.floorDiv(worldStartY, tileSize);
        int endRow = Math.floorDiv(worldStartY + EnginePanel.HEIGHT, tileSize);

        for (int row = startRow; row <= endRow; row++) {
            if (!world.isValidY(row)) continue;

            for (int col = startCol; col <= endCol; col++) {
                TileType fgTile = world.getBlock(col, row);
                TileType bgTile = world.getBackgroundBlock(col, row);

                int finalLight = calculateTileLight(world, col, row, playerTileX, playerTileY, activeLightRadius, activeLightIntensity);
                int screenX = (col * tileSize) - playerWorldX + playerScreenX;
                int screenY = (row * tileSize) - playerWorldY + playerScreenY;

                if (!fgTile.isSolid()) {
                    if (bgTile == TileType.AIR) {
                        g2.setColor(SKY_COLOR);
                        g2.fillRect(screenX, screenY, tileSize, tileSize);
                    } else if (bgTile.getSprite() != null) {
                        g2.drawImage(bgTile.getSprite(), screenX, screenY, tileSize, tileSize, null);
                    }

                    int bgLight = (bgTile == TileType.AIR) ? world.getSunLightLevel() : finalLight;
                    applyShadow(g2, screenX, screenY, tileSize, bgLight);
                }

                if (fgTile != TileType.AIR && fgTile.getSprite() != null) {
                    int offsetY = (fgTile == TileType.TORCH && world.isSolid(col, row + 1))
                            ? getTorchBottomOffset(fgTile.getSprite())
                            : 0;
                    g2.drawImage(fgTile.getSprite(), screenX, screenY + offsetY, tileSize, tileSize, null);

                    if (fgTile != TileType.TORCH) {
                        applyShadow(g2, screenX, screenY, tileSize, finalLight);
                    }
                }
            }
        }

        drawHoverAndMiningOverlay(g2, player, interaction, tileSize, playerWorldX, playerWorldY, playerScreenX, playerScreenY);
    }

    private int calculateTileLight(World world, int col, int row, int playerTileX, int playerTileY, double radius, int intensity) {
        int staticLight = world.getStaticLight(col, row);
        int finalLight = staticLight;

        double dx = col - playerTileX;
        double dy = row - playerTileY;
        double distSq = dx * dx + dy * dy;

        if (distSq < radius * radius) {
            double dist = Math.sqrt(distSq);
            int playerLight = (int) (intensity * (1.0 - (dist / radius)));
            finalLight = Math.max(staticLight, playerLight);
        }
        return finalLight;
    }

    private void applyShadow(Graphics2D g2, int x, int y, int size, int lightLevel) {
        int idx = Math.clamp(lightLevel, 0, 15);
        g2.setColor(SHADOW_COLORS[idx]);
        g2.fillRect(x, y, size, size);
    }

    private boolean isHoldingTorch(Player player) {
        Item held = player.hotBar.getCurrentItem();
        return held != null && held.getTileType() == TileType.TORCH && held.getQuantity() > 0;
    }

    private int getTorchBottomOffset(BufferedImage sprite) {
        if (cachedTorchBottomOffset != null) {
            return cachedTorchBottomOffset;
        }
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if (((sprite.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    cachedTorchBottomOffset = ((height - 1) - y) * EnginePanel.scale;
                    return cachedTorchBottomOffset;
                }
            }
        }
        cachedTorchBottomOffset = 0;
        return 0;
    }

    private void drawHoverAndMiningOverlay(Graphics2D g2, Player player, BlockInteractionController interaction,
                                           int tileSize, int playerWorldX, int playerWorldY, int playerScreenX, int playerScreenY) {
        int tileX = interaction.hoveredTile[0];
        int tileY = interaction.hoveredTile[1];
        if (tileX == -1) return;

        int screenX = tileX * tileSize - playerWorldX + playerScreenX;
        int screenY = tileY * tileSize - playerWorldY + playerScreenY;

        g2.setColor(player.isBackgroundMode() ? MOUSE_HOVER_BACKGROUND_COLOR : MOUSE_HOVER_FOREGROUND_COLOR);
        g2.fillRect(screenX, screenY, tileSize, tileSize);

        if (interaction.isMiningTile(tileX, tileY)) {
            drawMiningOverlay(g2, screenX, screenY, tileSize, interaction.getMiningRatio());
        }
    }

    private void drawMiningOverlay(Graphics2D g2, int screenX, int screenY, int tileSize, float ratio) {
        int stage = (int) (ratio * 4);
        int shakeX = (ratio < 1.0f) ? (int) (Math.random() * 3) - 1 : 0;
        int shakeY = (ratio < 1.0f) ? (int) (Math.random() * 3) - 1 : 0;

        Graphics2D g2d = (Graphics2D) g2.create();
        g2d.translate(shakeX, shakeY);

        g2d.setColor(new Color(0, 0, 0, 200));
        int cx = screenX + tileSize / 2;
        int cy = screenY + tileSize / 2;
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        if (stage >= 0) {
            g2d.drawLine(cx, cy, cx - 6, cy - 8);
            g2d.drawLine(cx, cy, cx + 8, cy + 6);
        }
        if (stage >= 1) {
            g2d.drawLine(cx - 6, cy - 8, screenX + 8, screenY + 6);
            g2d.drawLine(cx, cy, cx + 10, cy - 10);
        }
        if (stage >= 2) {
            g2d.drawLine(cx + 8, cy + 6, screenX + tileSize - 8, screenY + tileSize - 8);
            g2d.drawLine(cx, cy, cx - 10, cy + 10);
        }
        if (stage >= 3) {
            g2d.drawLine(cx + 10, cy - 10, screenX + tileSize - 6, screenY + 8);
            g2d.drawLine(cx - 10, cy + 10, screenX + 6, screenY + tileSize - 6);
            g2d.drawLine(screenX + 8, cy, screenX + tileSize - 8, cy);
        }

        int barWidth = tileSize - 12;
        int barHeight = 5;
        int barX = screenX + 6;
        int barY = screenY + tileSize - 9;

        g2d.setColor(new Color(20, 20, 20, 220));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.drawRect(barX, barY, barWidth, barHeight);

        int filledWidth = (int) (barWidth * ratio);
        Color barColor = ratio < 0.5f ? new Color(255, 220, 0)
                : (ratio < 0.8f ? new Color(255, 140, 0) : new Color(255, 50, 50));
        g2d.setColor(barColor);
        g2d.fillRect(barX + 1, barY + 1, Math.max(0, filledWidth - 1), barHeight - 1);

        g2d.dispose();
    }
}