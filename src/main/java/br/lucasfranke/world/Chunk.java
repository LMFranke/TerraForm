package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;

public class Chunk {
    public final int cx;
    public final TileType[][] blocks;
    public byte[][] lightMap;

    public Chunk(int cx, TileType[][] blocks) {
        this.cx = cx;
        this.blocks = blocks;
        this.lightMap = new byte[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
    }

    public void setBlock(int worldX, int worldY, TileType type) {
        int localX = worldX % EnginePanel.CHUNK_WIDTH;
        int localY = worldY % EnginePanel.CHUNK_HEIGHT;

        switch (blocks[localY][localX]) {
            case DIRT, GRASS -> EnginePanel.soundManager.playSound(Sound.GRASS_BREAK);
            case STONE -> EnginePanel.soundManager.playSound(Sound.STONE_BREAK);
        }
        blocks[localY][localX] = type;
    }

    public TileType getBlock(int worldX, int worldY) {
        int localX = worldX % EnginePanel.CHUNK_WIDTH;
        int localY = worldY % EnginePanel.CHUNK_HEIGHT;
        return blocks[localY][localX];
    }

}
