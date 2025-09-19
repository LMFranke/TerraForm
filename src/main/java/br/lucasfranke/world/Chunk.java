package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;

public class Chunk {
    public final int cx, cy;
    public final TileType[][] blocks;
    public int size = EnginePanel.chunkSize;

    public Chunk(int cx, int cy, TileType[][] blocks) {
        this.cx = cx;
        this.cy = cy;
        this.blocks = blocks;
    }

    public void setBlock(int worldX, int worldY, TileType type) {
        int localX = worldX % size;
        int localY = worldY % size;

        switch (blocks[localY][localX]) {
            case DIRT, GRASS -> EnginePanel.soundManager.playSound(Sound.GRASS_BREAK);
            case STONE -> EnginePanel.soundManager.playSound(Sound.STONE_BREAK);
        }
        blocks[localY][localX] = type;
    }

    public TileType getBlock(int worldX, int worldY) {
        int localX = worldX % size;
        int localY = worldY % size;
        return blocks[localY][localX];
    }

}
