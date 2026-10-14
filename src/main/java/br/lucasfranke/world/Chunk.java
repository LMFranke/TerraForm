package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;

public class Chunk {
    public final int cx;
    public final TileType[][] blocks;
    public final TileType[][] backgroundBlocks;
    public byte[][] lightMap;
    public int lastSunLightLevel = -1;

    public Chunk(int cx, TileType[][] blocks, TileType[][] backgroundBlocks) {
        this.cx = cx;
        this.blocks = blocks;
        this.backgroundBlocks = backgroundBlocks;
        this.lightMap = new byte[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
    }

    public void setBlock(int worldX, int worldY, TileType type) {
        if (type == null) {
            return;
        }
        int localX = Math.floorMod(worldX, EnginePanel.CHUNK_WIDTH);
        int localY = Math.floorMod(worldY, EnginePanel.CHUNK_HEIGHT);
        blocks[localY][localX] = type;
    }

    public TileType getBlock(int worldX, int worldY) {
        int localX = Math.floorMod(worldX, EnginePanel.CHUNK_WIDTH);
        int localY = Math.floorMod(worldY, EnginePanel.CHUNK_HEIGHT);
        return blocks[localY][localX];
    }

    public void setBackgroundBlock(int worldX, int worldY, TileType type) {
        if (type == null) {
            return;
        }
        int localX = Math.floorMod(worldX, EnginePanel.CHUNK_WIDTH);
        int localY = Math.floorMod(worldY, EnginePanel.CHUNK_HEIGHT);
        backgroundBlocks[localY][localX] = type;
    }

    public TileType getBackgroundBlock(int worldX, int worldY) {
        int localX = Math.floorMod(worldX, EnginePanel.CHUNK_WIDTH);
        int localY = Math.floorMod(worldY, EnginePanel.CHUNK_HEIGHT);
        return backgroundBlocks[localY][localX];
    }
}