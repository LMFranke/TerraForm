package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.util.Random;

public class WorldManager {

    private static final Color MOUSE_HOVER_COLOR = new Color(255, 255, 0, 120);
    private static final Color[] SHADOW_COLORS = new Color[16];

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
//        spawnPlayer();
    }

    public void spawnPlayer() {
        int xChunk = (int) (EnginePanel.player.worldX / EnginePanel.tileSquare / EnginePanel.CHUNK_WIDTH);

        int xPlayerChunk = (int) (EnginePanel.player.worldX % EnginePanel.CHUNK_WIDTH);
        int yPlayerChunk = (int) Math.abs(EnginePanel.player.worldY / EnginePanel.tileSquare);

        Chunk chunk = world.getChunk(xChunk);

        while (chunk.blocks[yPlayerChunk + 1][xPlayerChunk] != TileType.GRASS) {
            if (chunk.blocks[yPlayerChunk + 1][xPlayerChunk] == TileType.AIR) {
                yPlayerChunk++;
            } else {
                yPlayerChunk--;
            }
        }

        EnginePanel.player.worldY = ( EnginePanel.CHUNK_HEIGHT + yPlayerChunk) * EnginePanel.tileSquare;
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

        // Calculate the visible world's limit on screen
        int worldStartX = playerWorldX - playerScreenX;
        int worldStartY = playerWorldY - playerScreenY;
        int worldEndX = worldStartX + screenWidth;
        int worldEndY = worldStartY + screenHeight;

        // Convert the world coordinates to tile coordinates
        int startCol = worldStartX / tileSize;
        int endCol = worldEndX / tileSize;
        int startRow = worldStartY / tileSize;
        int endRow = worldEndY / tileSize;


        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {

                int chunkX = Math.floorDiv(col, EnginePanel.CHUNK_WIDTH);
                Chunk chunk = world.getChunk(chunkX);
                if (chunk == null) {
                    continue;
                }

                int tileXInChunk = Math.floorMod(col, EnginePanel.CHUNK_WIDTH);
                int tileYInChunk = Math.floorMod(row, EnginePanel.CHUNK_HEIGHT);

                if (tileYInChunk < 0 || tileYInChunk >= EnginePanel.CHUNK_HEIGHT) {
                    continue;
                }

                TileType tile = chunk.blocks[tileYInChunk][tileXInChunk];

                if (tile == null || tile.getSprite() == null) {
                    continue;
                }

                int screenX = (col * tileSize) - playerWorldX + playerScreenX;
                int screenY = (row * tileSize) - playerWorldY + playerScreenY;

                g2.drawImage(tile.getSprite(), screenX, screenY, tileSize, tileSize, null);

                byte lightLevel = chunk.lightMap[tileYInChunk][tileXInChunk];

                if ((int) lightLevel >= 0 && (int) lightLevel < SHADOW_COLORS.length) {
                    g2.setColor(SHADOW_COLORS[lightLevel]);
                    g2.fillRect(screenX, screenY, tileSize, tileSize);
                }

            }
        }

        if (hoveredTileWorldPosition[0] != -1) {
            int tileX = hoveredTileWorldPosition[0];
            int tileY = hoveredTileWorldPosition[1];

            int screenX = tileX * tileSize - playerWorldX + EnginePanel.player.playerX;
            int screenY = tileY * tileSize - playerWorldY + EnginePanel.player.playerY;

            g2.setColor(MOUSE_HOVER_COLOR);
            g2.fillRect(screenX, screenY, tileSize, tileSize);
        }
    }

    public void handleBlockClick() {
        int tileX = hoveredTileWorldPosition[0];
        int tileY = hoveredTileWorldPosition[1];

        int xChunk = tileX / EnginePanel.CHUNK_WIDTH;

        Chunk chunk = world.getChunk(xChunk);

        if (chunk == null) {
            return;
        }

        TileType current = chunk.getBlock(tileX, tileY);

        switch (current) {
            case GRASS -> chunk.setBlock(tileX, tileY, TileType.BACKGROUND_GRASS);
            case DIRT -> chunk.setBlock(tileX, tileY, TileType.BACKGROUND_DIRT);
            case STONE -> chunk.setBlock(tileX, tileY, TileType.BACKGROUND_STONE);
        }

        world.calculateLightForChunk(xChunk);

        int localX = tileX % EnginePanel.CHUNK_WIDTH;
        if (localX == 0) {
            world.calculateLightForChunk(xChunk - 1);
        } else if (localX == EnginePanel.CHUNK_WIDTH - 1) {
            world.calculateLightForChunk(xChunk + 1);
        }
    }
}