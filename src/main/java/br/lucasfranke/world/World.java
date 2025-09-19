package br.lucasfranke.world;

import br.lucasfranke.model.type.TileType;

import java.util.HashMap;
import java.util.Map;

public class World {

    private final Map<String, Chunk> chunks = new HashMap<>();
    private final int chunkSize;
    private final long seed;
    private final Noise1D terrainNoise;
    private final Noise2D caveNoise;

    public World(int chunkSize, long seed) {
        this.chunkSize = chunkSize;
        this.seed = seed;
        this.terrainNoise = new Noise1D(seed);
        this.caveNoise = new Noise2D(seed);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public Chunk getChunk(int cx, int cy) {
        String key = cx + "," + cy;
        if (!chunks.containsKey(key)) {
            Chunk c = generateChunk(cx, cy);
            chunks.put(key, c);
        }
        return chunks.get(key);
    }

    private Chunk generateChunk(int cx, int cy) {
        TileType[][] blocks = new TileType[chunkSize][chunkSize];

        int worldXStart = cx * chunkSize;
        int worldYStart = cy * chunkSize;

        for (int row = 0; row < chunkSize; row++) {
            for (int col = 0; col < chunkSize; col++) {
                int worldX = worldXStart + col;
                int worldY = worldYStart + row;

                int groundHeight = (int) (terrainNoise.sample(worldX * 0.05) * 15 + 40);

                int stoneHeight = groundHeight + 5;
                stoneHeight += (int)(terrainNoise.sample(worldX * 0.1) * 3);

                if (worldY > groundHeight && worldY <= stoneHeight) {
                    blocks[row][col] = TileType.DIRT;
                } else if (worldY > stoneHeight) {
                    blocks[row][col] = TileType.STONE;
                } else if (worldY == groundHeight) {
                    blocks[row][col] = TileType.GRASS;
                } else {
                    blocks[row][col] = TileType.AIR;
                }
            }
        }

        for (int row = 0; row < chunkSize; row++) {
            for (int col = 0; col < chunkSize; col++) {
                int worldX = worldXStart + col;
                int worldY = worldYStart + row;

                if (blocks[row][col] == TileType.STONE) {
                    double caveValue = caveNoise.sample(worldX * 0.1, worldY * 0.1);

                    if (caveValue > 0.55) {
                        blocks[row][col] = TileType.BACKGROUND_STONE;
                    }
                }
            }
        }


        TileType[][] newBlocks = new TileType[chunkSize][chunkSize];

        for (int row = 0; row < chunkSize; row++) {
            for (int col = 0; col < chunkSize; col++) {
                int neighbors = countStoneNeighbors(blocks, row, col);

                if (blocks[row][col] == TileType.BACKGROUND_STONE) {
                    newBlocks[row][col] = (neighbors > 5) ? TileType.STONE : TileType.BACKGROUND_STONE;
                } else if (blocks[row][col] == TileType.STONE) {
                    newBlocks[row][col] = (neighbors < 4) ? TileType.BACKGROUND_STONE : TileType.STONE;
                } else {
                    newBlocks[row][col] = blocks[row][col];
                }
            }
        }

        return new Chunk(cx, cy, blocks);
    }

    private int countStoneNeighbors(TileType[][] blocks, int row, int col) {
        int count = 0;

        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) continue;

                int nx = col + x;
                int ny = row + y;

                if (nx >= 0 && nx < chunkSize && ny >= 0 && ny < chunkSize) {
                    if (blocks[ny][nx] == TileType.STONE) {
                        count++;
                    }
                } else {
                    count++;
                }
            }
        }
        return count;
    }

//    // Exemplo de descarte simples
//    public void unloadChunk(int cx, int cy) {
//        chunks.remove(key(cx,cy));
//    }

}
