package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class WorldManager {

    private static final Color MOUSE_HOVER_FOREGROUND_COLOR = new Color(255, 255, 0, 120);
    private static final Color MOUSE_HOVER_BACKGROUND_COLOR = new Color(0, 200, 255, 130);
    public static final Color[] SHADOW_COLORS = new Color[16];

    private static final double BASE_PLAYER_LIGHT_RADIUS = 2.2;
    private static final int BASE_PLAYER_LIGHT_INTENSITY = 8;
    private static final double TORCH_HAND_LIGHT_RADIUS = 6.0;
    private static final int TORCH_HAND_LIGHT_INTENSITY = 14;

    private double timeOfDay = 8.0;
    private static final double TIME_INCREMENT_PER_TICK = 24.0 / (60.0 * 120.0);

    // Estado da mineração progressiva
    private int miningTileX = -1;
    private int miningTileY = -1;
    private boolean miningBackground = false;
    private int miningProgress = 0;
    private int miningTargetHardness = 0;
    private int breakCooldown = 0;

    static {
        for (int i = 0; i < 16; i++) {
            float opacity = 1.0f - (i / 15.0f);
            SHADOW_COLORS[i] = new Color(0, 0, 0, opacity);
        }
    }

    public final World world;
    public int[] hoveredTileWorldPosition = {-1, -1};

    public WorldManager() {
        world = new World(new Random().nextInt(100000));
    }

    public void spawnPlayerOnGround() {
        int playerTileX = (int) (EnginePanel.player.worldX / EnginePanel.tileSquare);
        int chunkX = Math.floorDiv(playerTileX, EnginePanel.CHUNK_WIDTH);

        Chunk spawnChunk = world.getChunk(chunkX);
        if (spawnChunk == null) {
            System.err.println("Failure while trying to spawn the player: Chunk not found.");
            return;
        }
        int groundY = 0;
        while (groundY < EnginePanel.CHUNK_HEIGHT - 1) {
            int localX = Math.floorMod(playerTileX, EnginePanel.CHUNK_WIDTH);
            TileType blockBelow = spawnChunk.blocks[groundY + 1][localX];
            if (blockBelow != null && blockBelow.isSolid()) {
                break;
            }
            groundY++;
        }

        EnginePanel.player.worldY = groundY * EnginePanel.tileSquare;
    }

    public void update() {
        TileType.updateAnimations();

        if (breakCooldown > 0) {
            breakCooldown--;
        }

        int tileSize = EnginePanel.tileSquare;
        int playerTileX = (int) EnginePanel.player.worldX / tileSize;
        int playerTileY = (int) EnginePanel.player.worldY / tileSize;

        updateDayNightCycle(playerTileX);

        int mouseWorldX = (int) (EnginePanel.player.worldX - EnginePanel.player.playerX + MouseManager.getX());
        int mouseWorldY = (int) (EnginePanel.player.worldY - EnginePanel.player.playerY + MouseManager.getY());

        int tileX = Math.floorDiv(mouseWorldX, tileSize);
        int tileY = Math.floorDiv(mouseWorldY, tileSize);

        int distance = Math.abs(tileX - playerTileX) + Math.abs(tileY - playerTileY);

        if (distance < 5) {
            hoveredTileWorldPosition[0] = tileX;
            hoveredTileWorldPosition[1] = tileY;
        } else {
            hoveredTileWorldPosition[0] = -1;
            hoveredTileWorldPosition[1] = -1;
            resetMining();
        }
    }

    public void resetMining() {
        miningTileX = -1;
        miningTileY = -1;
        miningProgress = 0;
        miningTargetHardness = 0;
    }

    private void updateDayNightCycle(int playerTileX) {
        if (KeyManager.isKeyPressed(KeyEvent.VK_T)) {
            timeOfDay = (timeOfDay + 1.0) % 24.0;
            KeyManager.unpressKey(KeyEvent.VK_T);
        } else {
            timeOfDay = (timeOfDay + TIME_INCREMENT_PER_TICK) % 24.0;
        }

        int targetSunLight = calculateSunLightForHour(timeOfDay);
        int playerChunkX = Math.floorDiv(playerTileX, EnginePanel.CHUNK_WIDTH);
        world.setSunLightLevel(targetSunLight, playerChunkX);
    }

    private int calculateSunLightForHour(double hour) {
        int minLight = World.MIN_NIGHT_LIGHT_LEVEL;
        int maxLight = World.MAX_LIGHT_LEVEL;

        if (hour >= 7.0 && hour < 17.0) {
            return maxLight;
        } else if (hour >= 17.0 && hour < 20.0) {
            double progress = (hour - 17.0) / 3.0;
            return (int) Math.round(maxLight - progress * (maxLight - minLight));
        } else if (hour >= 4.0 && hour < 7.0) {
            double progress = (hour - 4.0) / 3.0;
            return (int) Math.round(minLight + progress * (maxLight - minLight));
        } else {
            return minLight;
        }
    }

    public String getFormattedTime() {
        int hours = (int) timeOfDay;
        int minutes = (int) ((timeOfDay - hours) * 60);
        String period = (timeOfDay >= 6.0 && timeOfDay < 18.0) ? "Dia" : "Noite";
        return String.format("%02d:%02d (%s)", hours, minutes, period);
    }

    public void draw(Graphics2D g2) {
        int tileSize = EnginePanel.tileSquare;
        int screenWidth = EnginePanel.WIDTH;
        int screenHeight = EnginePanel.HEIGHT;

        int playerWorldX = (int) EnginePanel.player.worldX;
        int playerWorldY = (int) EnginePanel.player.worldY;
        int playerScreenX = EnginePanel.player.playerX;
        int playerScreenY = EnginePanel.player.playerY;

        int playerTileX = (playerWorldX + EnginePanel.player.width / 2) / tileSize;
        int playerTileY = (playerWorldY + EnginePanel.player.height / 2) / tileSize;

        boolean holdingTorch = false;
        Item heldItem = EnginePanel.player.hotBar.getCurrentItem();
        if (heldItem != null && heldItem.getTileType() == TileType.TORCH && heldItem.getQuantity() > 0) {
            holdingTorch = true;
        }

        double activeLightRadius = holdingTorch ? TORCH_HAND_LIGHT_RADIUS : BASE_PLAYER_LIGHT_RADIUS;
        int activeLightIntensity = holdingTorch ? TORCH_HAND_LIGHT_INTENSITY : BASE_PLAYER_LIGHT_INTENSITY;

        int worldStartX = playerWorldX - playerScreenX;
        int worldStartY = playerWorldY - playerScreenY;
        int worldEndX = worldStartX + screenWidth;
        int worldEndY = worldStartY + screenHeight;

        int startCol = Math.floorDiv(worldStartX, tileSize);
        int endCol = Math.floorDiv(worldEndX, tileSize);
        int startRow = Math.floorDiv(worldStartY, tileSize);
        int endRow = Math.floorDiv(worldEndY, tileSize);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                if (row < 0 || row >= EnginePanel.CHUNK_HEIGHT) {
                    continue;
                }
                Chunk chunk = world.getChunk(Math.floorDiv(col, EnginePanel.CHUNK_WIDTH));
                if (chunk == null) {
                    continue;
                }

                int tileXInChunk = Math.floorMod(col, EnginePanel.CHUNK_WIDTH);
                int tileYInChunk = row;

                TileType fgTile = chunk.blocks[tileYInChunk][tileXInChunk];
                TileType bgTile = chunk.backgroundBlocks[tileYInChunk][tileXInChunk];
                if (fgTile == null) fgTile = TileType.AIR;
                if (bgTile == null) bgTile = TileType.AIR;

                byte staticLightLevel = chunk.lightMap[tileYInChunk][tileXInChunk];
                byte finalLightLevel = staticLightLevel;

                double dx = col - playerTileX;
                double dy = row - playerTileY;
                double distanceSq = dx * dx + dy * dy;

                if (distanceSq < activeLightRadius * activeLightRadius) {
                    double distance = Math.sqrt(distanceSq);
                    double lightFalloff = distance / activeLightRadius;
                    int playerLightLevel = (int) (activeLightIntensity * (1 - lightFalloff));
                    finalLightLevel = (byte) Math.max(staticLightLevel, playerLightLevel);
                }

                int screenX = (col * tileSize) - playerWorldX + playerScreenX;
                int screenY = (row * tileSize) - playerWorldY + playerScreenY;

                if (fgTile == TileType.TORCH) {
                    if (bgTile.getSprite() != null) {
                        g2.drawImage(bgTile.getSprite(), screenX, screenY, tileSize, tileSize, null);
                    }
                    int bgShadow = (bgTile == TileType.AIR) ? world.getSunLightLevel() : finalLightLevel;
                    bgShadow = Math.max(0, Math.min(15, bgShadow));
                    g2.setColor(SHADOW_COLORS[bgShadow]);
                    g2.fillRect(screenX, screenY, tileSize, tileSize);

                    BufferedImage torchSprite = fgTile.getSprite();
                    if (torchSprite != null) {
                        int offsetY = isBlockSolid(col, row + 1) ? getBottomTransparentOffset(torchSprite) : 0;
                        g2.drawImage(torchSprite, screenX, screenY + offsetY, tileSize, tileSize, null);
                    }
                    continue;
                }

                if (fgTile == TileType.AIR) {
                    if (bgTile.getSprite() != null) {
                        g2.drawImage(bgTile.getSprite(), screenX, screenY, tileSize, tileSize, null);
                    }
                    int shadowIndex = (bgTile == TileType.AIR) ? world.getSunLightLevel() : finalLightLevel;
                    shadowIndex = Math.max(0, Math.min(15, shadowIndex));
                    g2.setColor(SHADOW_COLORS[shadowIndex]);
                    g2.fillRect(screenX, screenY, tileSize, tileSize);
                    continue;
                }

                if (fgTile.getSprite() != null) {
                    g2.drawImage(fgTile.getSprite(), screenX, screenY, tileSize, tileSize, null);
                }
                int shadowIndex = Math.max(0, Math.min(15, finalLightLevel));
                g2.setColor(SHADOW_COLORS[shadowIndex]);
                g2.fillRect(screenX, screenY, tileSize, tileSize);
            }
        }

        if (hoveredTileWorldPosition[0] != -1) {
            int tileX = hoveredTileWorldPosition[0];
            int tileY = hoveredTileWorldPosition[1];
            int screenX = tileX * tileSize - playerWorldX + playerScreenX;
            int screenY = tileY * tileSize - playerWorldY + playerScreenY;

            g2.setColor(EnginePanel.player.isBackgroundMode()
                    ? MOUSE_HOVER_BACKGROUND_COLOR
                    : MOUSE_HOVER_FOREGROUND_COLOR);
            g2.fillRect(screenX, screenY, tileSize, tileSize);

            if (miningProgress > 0 && miningTargetHardness > 1 && tileX == miningTileX && tileY == miningTileY) {
                drawMiningOverlay(g2, screenX, screenY, tileSize);
            }
        }
    }

    private void drawMiningOverlay(Graphics2D g2, int screenX, int screenY, int tileSize) {
        float ratio = Math.min(1.0f, (float) miningProgress / miningTargetHardness);
        int stage = (int) (ratio * 4);

        int shakeX = 0;
        int shakeY = 0;
        if (miningProgress < miningTargetHardness) {
            shakeX = (int) (Math.random() * 3) - 1;
            shakeY = (int) (Math.random() * 3) - 1;
        }

        Graphics2D g2d = (Graphics2D) g2.create();
        g2d.translate(shakeX, shakeY);
        g2d.setColor(newColorWithAlpha(Color.BLACK, 200));
        int cx = screenX + tileSize / 2;
        int cy = screenY + tileSize / 2;

        Stroke oldStroke = g2d.getStroke();
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

        g2d.setStroke(oldStroke);

        int barWidth = tileSize - 12;
        int barHeight = 5;
        int barX = screenX + 6;
        int barY = screenY + tileSize - 9;

        g2d.setColor(new Color(20, 20, 20, 220));
        g2d.fillRect(barX, barY, barWidth, barHeight);
        g2d.setColor(new Color(100, 100, 100, 200));
        g2d.drawRect(barX, barY, barWidth, barHeight);

        int filledWidth = (int) (barWidth * ratio);
        Color barColor = ratio < 0.5f ? new Color(255, 220, 0) : (ratio < 0.8f ? new Color(255, 140, 0) : new Color(255, 50, 50));
        g2d.setColor(barColor);
        g2d.fillRect(barX + 1, barY + 1, filledWidth - 1, barHeight - 1);

        g2d.dispose();
    }

    private Color newColorWithAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private int getBottomTransparentOffset(BufferedImage sprite) {
        int width = sprite.getWidth();
        int height = sprite.getHeight();
        for (int y = height - 1; y >= 0; y--) {
            for (int x = 0; x < width; x++) {
                if (((sprite.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    int emptyRowsAtBottom = (height - 1) - y;
                    return emptyRowsAtBottom * EnginePanel.scale;
                }
            }
        }
        return 0;
    }

    private boolean isBlockSolid(int worldTileX, int worldTileY) {
        if (worldTileY < 0 || worldTileY >= EnginePanel.CHUNK_HEIGHT) {
            return false;
        }
        Chunk c = world.getChunk(Math.floorDiv(worldTileX, EnginePanel.CHUNK_WIDTH));
        if (c == null) {
            return false;
        }
        int localX = Math.floorMod(worldTileX, EnginePanel.CHUNK_WIDTH);
        TileType type = c.blocks[worldTileY][localX];
        return type != null && type.isSolid();
    }

    public void handleBlockInteraction() {
        if (hoveredTileWorldPosition[0] == -1) {
            resetMining();
            return;
        }
        int tileX = hoveredTileWorldPosition[0];
        int tileY = hoveredTileWorldPosition[1];
        if (tileY < 0 || tileY >= EnginePanel.CHUNK_HEIGHT) {
            resetMining();
            return;
        }

        int xChunk = Math.floorDiv(tileX, EnginePanel.CHUNK_WIDTH);
        Chunk chunk = world.getChunk(xChunk);

        if (chunk == null) {
            return;
        }

        boolean backgroundMode = EnginePanel.player.isBackgroundMode();

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
            if (breakCooldown > 0) {
                return;
            }
            if (handleBreakProgress(chunk, tileX, tileY, backgroundMode)) {
                world.calculateLightForChunkAndNeighbors(xChunk);
            }
        } else if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
            if (!handlePlaceClick(chunk, tileX, tileY, backgroundMode)) {
                world.calculateLightForChunkAndNeighbors(xChunk);
            }
        }
    }

    private boolean handleBreakProgress(Chunk chunk, int tileX, int tileY, boolean backgroundMode) {
        TileType currentFg = chunk.getBlock(tileX, tileY);
        boolean breakingForeground = (currentFg != null && currentFg != TileType.AIR && currentFg.isBreakable());

        TileType targetTile = null;
        boolean targetIsBg = false;

        if (breakingForeground) {
            targetTile = currentFg;
        } else if (backgroundMode) {
            TileType currentBg = chunk.getBackgroundBlock(tileX, tileY);
            TileType naturalBg = chunk.getOriginalBackgroundBlock(tileX, tileY);
            if (currentBg != null && currentBg != TileType.AIR && currentBg != naturalBg) {
                targetTile = currentBg;
                targetIsBg = true;
            }
        }

        if (targetTile == null) {
            resetMining();
            return false;
        }

        if (tileX != miningTileX || tileY != miningTileY || targetIsBg != miningBackground) {
            miningTileX = tileX;
            miningTileY = tileY;
            miningBackground = targetIsBg;
            miningProgress = 0;
            miningTargetHardness = Math.max(1, targetTile.getHardness());
        }

        miningProgress++;

        if (miningProgress % 15 == 0 && miningProgress < miningTargetHardness) {
            playBreakSound(targetTile);
        }

        if (miningProgress >= miningTargetHardness) {
            executeBlockBreak(chunk, tileX, tileY, targetTile, targetIsBg);
            resetMining();
            breakCooldown = 6;
            return true;
        }
        return false;
    }

    private void executeBlockBreak(Chunk chunk, int tileX, int tileY, TileType targetTile, boolean targetIsBg) {
        playBreakSound(targetTile);

        TileType drop = targetTile.getDropType();
        if (drop != TileType.AIR) {
            EnginePanel.player.addItem(drop, 1);
        }

        if (!targetIsBg) {
            chunk.setBlock(tileX, tileY, TileType.AIR);

            if (tileY > 0) {
                TileType blockAbove = chunk.getBlock(tileX - 1, tileY);
                TileType bgAbove = chunk.getBackgroundBlock(tileX - 1, tileY);
                if (blockAbove == TileType.TORCH && (bgAbove == null || bgAbove == TileType.AIR)) {
                    chunk.setBlock(tileX, tileY - 1, TileType.AIR);
                    EnginePanel.player.addItem(TileType.TORCH, 1);
                }
            }
        } else {
            TileType naturalBg = chunk.getOriginalBackgroundBlock(tileX, tileY);
            chunk.setBackgroundBlock(tileX, tileY, naturalBg != null ? naturalBg : TileType.AIR);
        }
    }

    private void playBreakSound(TileType tile) {
        switch (tile) {
            case DIRT, GRASS, BACKGROUND_DIRT, BACKGROUND_GRASS, TORCH -> EnginePanel.soundManager.playSound(Sound.GRASS_BREAK);
            case STONE, BACKGROUND_STONE -> EnginePanel.soundManager.playSound(Sound.STONE_BREAK);
        }
    }

    private boolean handlePlaceClick(Chunk chunk, int tileX, int tileY, boolean backgroundMode) {
        Item selectedItem = EnginePanel.player.hotBar.getCurrentItem();
        if (selectedItem == null || selectedItem.getQuantity() <= 0) return true;

        TileType tileToPlace = selectedItem.getTileType();
        if (!tileToPlace.isPlaceable()) {
            return true;
        }

        int localX = Math.floorMod(tileX, EnginePanel.CHUNK_WIDTH);
        int localY = Math.floorMod(tileY, EnginePanel.CHUNK_HEIGHT);

        TileType currentFg = chunk.blocks[localY][localX];
        TileType currentBg = chunk.backgroundBlocks[localY][localX];
        TileType naturalBg = chunk.originalBackgroundBlocks[localY][localX];

        if (currentFg != null && currentFg != TileType.AIR) {
            return true;
        }

        if (backgroundMode && tileToPlace != TileType.TORCH) {
            TileType bgVariant = getBackgroundVariant(tileToPlace);
            if (bgVariant != TileType.AIR) {
                boolean isEmptySky = (currentBg == null || currentBg == TileType.AIR);
                boolean isReplacingNaturalBg = (currentBg == naturalBg && currentBg != bgVariant);

                if (isEmptySky || isReplacingNaturalBg) {
                    chunk.setBackgroundBlock(tileX, tileY, bgVariant);
                    consumeSelectedItem(selectedItem);
                    return false;
                }
            }
        }

        if (tileToPlace == TileType.TORCH) {
            boolean hasBackgroundWall = (currentBg != null && currentBg != TileType.AIR);
            boolean hasSolidGroundBelow = isBlockSolid(tileX, tileY + 1);
            if (!hasBackgroundWall && !hasSolidGroundBelow) {
                return true;
            }
        }

        if (tileToPlace.isSolid()) {
            Rectangle targetBounds = new Rectangle(tileX * EnginePanel.tileSquare, tileY * EnginePanel.tileSquare, EnginePanel.tileSquare, EnginePanel.tileSquare);
            Rectangle playerBounds = new Rectangle((int) EnginePanel.player.worldX, (int) EnginePanel.player.worldY, EnginePanel.player.width, EnginePanel.player.height);

            if (playerBounds.intersects(targetBounds)) {
                return true;
            }
        }

        chunk.setBlock(tileX, tileY, tileToPlace);
        consumeSelectedItem(selectedItem);
        return false;
    }

    private void consumeSelectedItem(Item selectedItem) {
        selectedItem.decreaseQuantity();
        if (selectedItem.getQuantity() <= 0) {
            EnginePanel.player.hotBar.setItem(0, EnginePanel.player.hotBar.getSelectedSlot(), null);
        }
    }

    private TileType getBackgroundVariant(TileType type) {
        return switch (type) {
            case GRASS -> TileType.BACKGROUND_GRASS;
            case DIRT -> TileType.BACKGROUND_DIRT;
            case STONE -> TileType.BACKGROUND_STONE;
            default -> TileType.AIR;
        };
    }
}