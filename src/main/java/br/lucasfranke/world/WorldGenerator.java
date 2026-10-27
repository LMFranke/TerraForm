package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;

public class WorldGenerator {

    private static final int WORLD_Y_OFFSET = 10;
    private static final int MIN_TREE_SPACING = 4;

    private final long seed;
    private final Noise1D terrainNoise;
    private final Noise2D caveNoise;
    private final Noise2D coalNoise;
    private final Noise2D ironNoise;
    private final Noise2D goldNoise;

    public WorldGenerator(long seed) {
        this.seed = seed;
        this.terrainNoise = new Noise1D(seed);
        this.caveNoise = new Noise2D(seed);
        this.coalNoise = new Noise2D(seed + 1013L);
        this.ironNoise = new Noise2D(seed + 2027L);
        this.goldNoise = new Noise2D(seed + 3049L);
    }

    public Chunk generateChunk(int cx) {
        int height = EnginePanel.CHUNK_HEIGHT;
        int width = EnginePanel.CHUNK_WIDTH;
        TileType[][] blocks = new TileType[height][width];
        TileType[][] backgroundBlocks = new TileType[height][width];
        int[] surfaceRows = new int[width];
        int[] stoneStartRows = new int[width];

        int worldXStart = cx * width;

        generateBaseTerrain(blocks, backgroundBlocks, surfaceRows, stoneStartRows, worldXStart);
        TileType[][] smoothedBlocks = carveCavesAndSmooth(blocks, backgroundBlocks, worldXStart);
        generateOres(smoothedBlocks, stoneStartRows, worldXStart);

        Chunk chunk = new Chunk(cx, smoothedBlocks, backgroundBlocks);
        generateSurfaceTrees(chunk, surfaceRows, worldXStart);

        return chunk;
    }

    private void generateBaseTerrain(TileType[][] blocks, TileType[][] backgroundBlocks,
                                     int[] surfaceRows, int[] stoneStartRows, int worldXStart) {
        for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
            int worldX = worldXStart + col;
            int groundHeight = (int) (terrainNoise.sample(worldX * 0.05) * 15 + 40);
            int stoneHeight = groundHeight + 5 + (int) (terrainNoise.sample(worldX * 0.1) * 3);

            surfaceRows[col] = groundHeight - WORLD_Y_OFFSET;
            stoneStartRows[col] = stoneHeight - WORLD_Y_OFFSET;

            for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
                int worldY = WORLD_Y_OFFSET + row;

                if (worldY > groundHeight && worldY <= stoneHeight) {
                    blocks[row][col] = TileType.DIRT;
                    backgroundBlocks[row][col] = TileType.BACKGROUND_DIRT;
                } else if (worldY > stoneHeight) {
                    blocks[row][col] = TileType.STONE;
                    backgroundBlocks[row][col] = TileType.BACKGROUND_STONE;
                } else if (worldY == groundHeight) {
                    blocks[row][col] = TileType.GRASS;
                    backgroundBlocks[row][col] = TileType.BACKGROUND_GRASS;
                } else {
                    blocks[row][col] = TileType.AIR;
                    backgroundBlocks[row][col] = TileType.AIR;
                }
            }
        }
    }

    private TileType[][] carveCavesAndSmooth(TileType[][] blocks, TileType[][] backgroundBlocks, int worldXStart) {
        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
                if (blocks[row][col] == TileType.STONE) {
                    int worldX = worldXStart + col;
                    int worldY = WORLD_Y_OFFSET + row;
                    double caveValue = caveNoise.sample(worldX * 0.1, worldY * 0.1);
                    if (caveValue > 0.55) {
                        blocks[row][col] = TileType.AIR;
                    }
                }
            }
        }

        TileType[][] newBlocks = new TileType[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
                int neighbors = countStoneNeighbors(blocks, row, col);
                if (blocks[row][col] == TileType.AIR && backgroundBlocks[row][col] == TileType.BACKGROUND_STONE) {
                    newBlocks[row][col] = (neighbors > 5) ? TileType.STONE : TileType.AIR;
                } else if (blocks[row][col] == TileType.STONE) {
                    newBlocks[row][col] = (neighbors < 4) ? TileType.AIR : TileType.STONE;
                } else {
                    newBlocks[row][col] = blocks[row][col];
                }
            }
        }
        return newBlocks;
    }

    private void generateOres(TileType[][] blocks, int[] stoneStartRows, int worldXStart) {
        for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
            int worldX = worldXStart + col;
            int stoneStartRow = stoneStartRows[col];

            for (int row = Math.max(0, stoneStartRow + 2); row < EnginePanel.MAX_WORLD_HEIGHT; row++) {
                if (blocks[row][col] != TileType.STONE) {
                    continue;
                }

                int depthBelowStone = row - stoneStartRow;
                int worldY = WORLD_Y_OFFSET + row;

                if (depthBelowStone >= 42) {
                    double goldVal = goldNoise.sample(worldX * 0.18, worldY * 0.18);
                    if (goldVal > 0.79) {
                        blocks[row][col] = TileType.GOLD_ORE;
                        continue;
                    }
                }

                if (depthBelowStone >= 12) {
                    double ironVal = ironNoise.sample(worldX * 0.15, worldY * 0.15);
                    if (ironVal > 0.75) {
                        blocks[row][col] = TileType.IRON_ORE;
                        continue;
                    }
                }

                if (depthBelowStone >= 2) {
                    double coalVal = coalNoise.sample(worldX * 0.13, worldY * 0.13);
                    if (coalVal > 0.71) {
                        blocks[row][col] = TileType.COAL_ORE;
                    }
                }
            }
        }
    }

    private void generateSurfaceTrees(Chunk chunk, int[] surfaceRows, int worldXStart) {
        int lastTreeCol = -MIN_TREE_SPACING;

        for (int col = 2; col < EnginePanel.CHUNK_WIDTH - 2; col++) {
            if (col - lastTreeCol < MIN_TREE_SPACING) {
                continue;
            }

            int groundRow = surfaceRows[col];
            if (groundRow <= 10 || groundRow >= EnginePanel.CHUNK_HEIGHT - 1) {
                continue;
            }

            if (chunk.blocks[groundRow][col] != TileType.GRASS) {
                continue;
            }

            int worldX = worldXStart + col;
            int hash = deterministicHash(worldX);
            if ((hash & 0xFF) < 56) {
                int trunkHeight = 4 + (Math.abs(hash >> 8) % 3); // Height between 4 and 6 tiles
                placeTreeInBackground(chunk, col, groundRow - 1, trunkHeight);
                lastTreeCol = col;
            }
        }
    }

    private void placeTreeInBackground(Chunk chunk, int centerCol, int baseRow, int trunkHeight) {
        int topRow = baseRow - trunkHeight + 1;
        if (topRow < 3) {
            return;
        }

        for (int dy = -2; dy <= 1; dy++) {
            int leafRow = topRow + dy;
            if (leafRow < 0 || leafRow >= EnginePanel.CHUNK_HEIGHT) continue;

            int radius = (dy == -2) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int leafCol = centerCol + dx;
                if (leafCol >= 0 && leafCol < EnginePanel.CHUNK_WIDTH) {
                    if (chunk.blocks[leafRow][leafCol] == TileType.AIR
                            && chunk.backgroundBlocks[leafRow][leafCol] == TileType.AIR) {
                        chunk.backgroundBlocks[leafRow][leafCol] = TileType.BACKGROUND_LEAVES;
                        chunk.naturalTreeTiles[leafRow][leafCol] = true;
                    }
                }
            }
        }

        for (int r = baseRow; r >= topRow; r--) {
            if (chunk.blocks[r][centerCol] == TileType.AIR) {
                chunk.backgroundBlocks[r][centerCol] = TileType.BACKGROUND_WOOD;
                chunk.naturalTreeTiles[r][centerCol] = true;
            }
        }
    }

    private int deterministicHash(int worldX) {
        long h = seed ^ (worldX * 0x9E3779B97F4A7C15L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return (int) (h ^ (h >>> 31));
    }

    private int countStoneNeighbors(TileType[][] blocks, int row, int col) {
        int count = 0;
        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) continue;
                int nx = col + x;
                int ny = row + y;
                if (nx >= 0 && nx < EnginePanel.CHUNK_WIDTH && ny >= 0 && ny < EnginePanel.CHUNK_HEIGHT) {
                    if (blocks[ny][nx] == TileType.STONE) count++;
                } else {
                    count++;
                }
            }
        }
        return count;
    }
}