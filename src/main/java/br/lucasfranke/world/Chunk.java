package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;

public class Chunk {
    public final int cx;
    public final TileType[][] blocks;
    public final TileType[][] originalBlocks;
    public byte[][] lightMap;

    public Chunk(int cx, TileType[][] blocks) {
        this.cx = cx;
        this.blocks = blocks;
        this.lightMap = new byte[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
        this.originalBlocks = new TileType[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
        for (int i = 0; i < blocks.length; i++) {
            System.arraycopy(blocks[i], 0, this.originalBlocks[i], 0, blocks[i].length);
        }
    }

    public void setBlock(int worldX, int worldY, TileType type) {
        if (type == null) {
            System.out.println("setBlock: type is null");
            return;
        }

        int localX = worldX % EnginePanel.CHUNK_WIDTH;
        int localY = worldY % EnginePanel.CHUNK_HEIGHT;
        blocks[localY][localX] = type;
    }

    public TileType getBlock(int worldX, int worldY) {
        int localX = worldX % EnginePanel.CHUNK_WIDTH;
        int localY = worldY % EnginePanel.CHUNK_HEIGHT;
        return blocks[localY][localX];
    }

}
