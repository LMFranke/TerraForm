package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Random;

public class WorldManager {

    private static final Color MOUSE_HOVER_COLOR = new Color(255, 255, 0, 120);
    public static final Color[] SHADOW_COLORS = new Color[16];

    private static final double PLAYER_LIGHT_RADIUS = 3.5;
    public static final int PLAYER_LIGHT_INTENSITY = 13;


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
        int tileSize = EnginePanel.tileSquare;

        int mouseWorldX = (int) (EnginePanel.player.worldX - EnginePanel.player.playerX + MouseManager.getX());
        int mouseWorldY = (int) (EnginePanel.player.worldY - EnginePanel.player.playerY + MouseManager.getY());

        int tileX = mouseWorldX / tileSize;
        int tileY = mouseWorldY / tileSize;

        int playerTileX = (int) EnginePanel.player.worldX / tileSize;
        int playerTileY = (int) EnginePanel.player.worldY / tileSize;
        int distance = Math.abs(tileX - playerTileX) + Math.abs(tileY - playerTileY);

        if (distance < 5) {
            hoveredTileWorldPosition[0] = tileX;
            hoveredTileWorldPosition[1] = tileY;
        } else {
            hoveredTileWorldPosition[0] = -1;
            hoveredTileWorldPosition[1] = -1;
        }
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

        int worldStartX = playerWorldX - playerScreenX;
        int worldStartY = playerWorldY - playerScreenY;
        int worldEndX = worldStartX + screenWidth;
        int worldEndY = worldStartY + screenHeight;

        int startCol = worldStartX / tileSize;
        int endCol = worldEndX / tileSize;
        int startRow = worldStartY / tileSize;
        int endRow = worldEndY / tileSize;

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                Chunk chunk = world.getChunk(Math.floorDiv(col, EnginePanel.CHUNK_WIDTH));

                if (chunk == null) {
                    continue;
                }

                int tileXInChunk = Math.floorMod(col, EnginePanel.CHUNK_WIDTH);
                int tileYInChunk = Math.floorMod(row, EnginePanel.CHUNK_HEIGHT);

                if (tileYInChunk < 0 || tileYInChunk >= EnginePanel.CHUNK_HEIGHT) {
                    continue;
                }

                TileType tile = chunk.blocks[tileYInChunk][tileXInChunk];
                if (tile == null) {
                    continue;
                }

                byte staticLightLevel = chunk.lightMap[tileYInChunk][tileXInChunk];
                byte finalLightLevel = staticLightLevel;

                double dx = col - playerTileX;
                double dy = row - playerTileY;
                double distancePlayerLight = dx * dx + dy * dy;

                if (distancePlayerLight < PLAYER_LIGHT_RADIUS * PLAYER_LIGHT_RADIUS) {
                    double distance = Math.sqrt(distancePlayerLight);

                    double lightFalloff = distance / PLAYER_LIGHT_RADIUS;
                    int playerLightLevel = (int) (PLAYER_LIGHT_INTENSITY * (1 - lightFalloff));

                    finalLightLevel = (byte) Math.max(staticLightLevel, playerLightLevel);
                }

                int screenX = (col * tileSize) - playerWorldX + playerScreenX;
                int screenY = (row * tileSize) - playerWorldY + playerScreenY;

                if (tile.getSprite() != null) {
                    g2.drawImage(tile.getSprite(), screenX, screenY, tileSize, tileSize, null);
                }

                int shadowIndex = finalLightLevel;

                if (shadowIndex < 0) {
                    shadowIndex = 0;
                }

                if (shadowIndex > 15) {
                    shadowIndex = 15;
                }

                g2.setColor(SHADOW_COLORS[shadowIndex]);
                g2.fillRect(screenX, screenY, tileSize, tileSize);
            }
        }

        if (hoveredTileWorldPosition[0] != -1) {
            int tileX = hoveredTileWorldPosition[0];
            int tileY = hoveredTileWorldPosition[1];
            int screenX = tileX * tileSize - playerWorldX + playerScreenX;
            int screenY = tileY * tileSize - playerWorldY + playerScreenY;
            g2.setColor(MOUSE_HOVER_COLOR);
            g2.fillRect(screenX, screenY, tileSize, tileSize);
        }
    }


    /**
     * Handles all player interactions with the world grid, such as breaking and placing blocks.
     * This method should be called once per frame in the update loop.
     */
    public void handleBlockInteraction() {
        if (hoveredTileWorldPosition[0] == -1) {
            return;
        }

        int tileX = hoveredTileWorldPosition[0];
        int tileY = hoveredTileWorldPosition[1];

        int xChunk = Math.floorDiv(tileX, EnginePanel.CHUNK_WIDTH);
        Chunk chunk = world.getChunk(xChunk);
        if (chunk == null) {
            return;
        }

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
            handleBreakClick(chunk, tileX, tileY);
        } else if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
            if (handlePlaceClick(chunk, tileX, tileY)) return;
        }
        world.calculateLightForChunkAndNeighbors(xChunk);
    }

    private void handleBreakClick(Chunk chunk, int tileX, int tileY) {
        int localX = Math.floorMod(tileX, EnginePanel.CHUNK_WIDTH);
        int localY = Math.floorMod(tileY, EnginePanel.CHUNK_HEIGHT);

        TileType currentBlock = chunk.blocks[localY][localX];
        TileType naturalBlock = chunk.originalBlocks[localY][localX];

        if (!currentBlock.isSolid()) {
            return;
        }

        switch (currentBlock) {
            case DIRT, GRASS -> EnginePanel.soundManager.playSound(Sound.GRASS_BREAK);
            case STONE -> EnginePanel.soundManager.playSound(Sound.STONE_BREAK);
        }

        TileType newBlock = currentBlock == naturalBlock
                ? getBackgroundVariant(currentBlock)
                : naturalBlock.isSolid()
                ? getBackgroundVariant(naturalBlock)
                : naturalBlock;

        chunk.setBlock(tileX, tileY, newBlock);
    }

    private boolean handlePlaceClick(Chunk chunk, int tileX, int tileY) {
        Item selectedItem = EnginePanel.player.hotBar.getCurrentItem();
        if (selectedItem == null || selectedItem.getQuantity() <= 0) return true;

        TileType tileToPlace = selectedItem.getTileType();
        TileType currentBlock = chunk.getBlock(tileX, tileY);

        if (currentBlock.isSolid() || !tileToPlace.isSolid()) {
            return true;
        }

        Rectangle targetBounds = new Rectangle(tileX * EnginePanel.tileSquare, tileY * EnginePanel.tileSquare, EnginePanel.tileSquare, EnginePanel.tileSquare);
        Rectangle playerBounds = new Rectangle((int)EnginePanel.player.worldX, (int)EnginePanel.player.worldY, EnginePanel.player.width, EnginePanel.player.height);
        if (playerBounds.intersects(targetBounds)) {
            return true;
        }

        chunk.setBlock(tileX, tileY, tileToPlace);
        selectedItem.decreaseQuantity();

        if (selectedItem.getQuantity() <= 0) {
            EnginePanel.player.hotBar.setItem(0, EnginePanel.player.hotBar.getSelectedSlot(), null);
        }
        return false;
    }

    private TileType getBackgroundVariant(TileType type) {
        return switch (type) {
            case GRASS -> TileType.BACKGROUND_GRASS;
            case DIRT -> TileType.BACKGROUND_DIRT;
            case STONE -> TileType.BACKGROUND_STONE;
            default -> null;
        };
    }

}