package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.type.TileType;

import java.awt.*;

public class WorldManager {

    private static final Color MOUSE_HOVER_COLOR = new Color(255, 255, 0, 120);

    public final World world;

    public int[] hoveredTileWorldPosition = {-1, -1};

    public WorldManager() {
        world = new World(16, 999991L);
        spawnPlayer();
    }

    public void spawnPlayer() {
        int xChunk = (int) (EnginePanel.player.worldX / EnginePanel.tileSquare / world.getChunkSize());
        int yChunk = (int) (EnginePanel.player.worldY / EnginePanel.tileSquare / world.getChunkSize());

        int xPlayerChunk = (int) Math.abs(xChunk * world.getChunkSize() - EnginePanel.player.worldX / EnginePanel.tileSquare);
        int yPlayerChunk = (int) Math.abs(yChunk * world.getChunkSize() - EnginePanel.player.worldY / EnginePanel.tileSquare);

        Chunk chunk = world.getChunk(xChunk, yChunk);

        while (chunk.blocks[yPlayerChunk + 1][xPlayerChunk] != TileType.GRASS) {
            if (chunk.blocks[yPlayerChunk + 1][xPlayerChunk] == TileType.AIR) {
                yPlayerChunk++;
            } else {
                yPlayerChunk--;
            }
        }

        EnginePanel.player.worldY = (yChunk * world.getChunkSize() + yPlayerChunk) * EnginePanel.tileSquare;
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

        int chunkSize = world.getChunkSize();
        int chunkX = playerWorldX / (chunkSize * tileSize);
        int chunkY = playerWorldY / (chunkSize * tileSize);

        for (int cx = chunkX - 1; cx <= chunkX + 1; cx++) {
            for (int cy = chunkY - 1; cy <= chunkY + 1; cy++) {
                Chunk chunk = world.getChunk(cx, cy);
                if (chunk == null) continue;

                for (int row = 0; row < chunk.blocks.length; row++) {
                    for (int col = 0; col < chunk.blocks[0].length; col++) {
                        TileType tile = chunk.blocks[row][col];
                        if (tile == null || tile.getSprite() == null) continue;

                        int worldX = cx * chunkSize * tileSize + col * tileSize;
                        int worldY = cy * chunkSize * tileSize + row * tileSize;

                        int screenX = worldX - playerWorldX + EnginePanel.player.playerX;
                        int screenY = worldY - playerWorldY + EnginePanel.player.playerY;

                        if (screenX + tileSize < 0 || screenX > screenWidth ||
                                screenY + tileSize < 0 || screenY > screenHeight) {
                            continue;
                        }

                        g2.drawImage(tile.getSprite(), screenX, screenY, tileSize, tileSize, null);
                    }
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

        int xChunk = tileX / world.getChunkSize();
        int yChunk = tileY / world.getChunkSize();

        Chunk chunk = world.getChunk(xChunk, yChunk);
        if (chunk == null) return;

        TileType current = chunk.getBlock(tileX, tileY);

        switch (current) {
            case GRASS -> chunk.setBlock(tileX, tileY, TileType.BACKGROUND_GRASS);
            case DIRT -> chunk.setBlock(tileX, tileY, TileType.BACKGROUND_DIRT);
            case STONE -> chunk.setBlock(tileX, tileY, TileType.BACKGROUND_STONE);
        }
    }
}