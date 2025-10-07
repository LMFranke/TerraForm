package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class World {

    private final Map<String, Chunk> chunks = new HashMap<>();
    private final long seed;
    private final Noise1D terrainNoise;
    private final Noise2D caveNoise;
    private static final int MAX_LIGHT_LEVEL = 15;

    public World(long seed) {
        this.seed = seed;
        this.terrainNoise = new Noise1D(seed);
        this.caveNoise = new Noise2D(seed);
    }

    public Chunk getChunk(int cx) {
        String key = cx + "";
        if (!chunks.containsKey(key)) {
            Chunk c = generateChunk(cx);
            chunks.put(key, c);

            calculateLightForChunk(cx);

            if (chunks.containsKey((cx - 1) + "")) {
                calculateLightForChunk(cx - 1);
            }
            if (chunks.containsKey((cx + 1) + "")) {
                calculateLightForChunk(cx + 1);
            }
        }
        return chunks.get(key);
    }

    private Chunk generateChunk(int cx) {
        TileType[][] blocks = new TileType[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];

        int worldXStart = cx * EnginePanel.CHUNK_WIDTH;
        int worldYStart = 10;

        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
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

        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
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


        TileType[][] newBlocks = new TileType[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];

        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
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

        return new Chunk(cx, newBlocks);
    }

    /**
     * Calculates lighting for a chunk, including neighbor chunks to prevent seams.
     * @param cx The X-coordinate of the chunk to update.
     */
    public void calculateLightForChunk(int cx) {
        Chunk mainChunk = getChunk(cx);
        Chunk leftChunk = chunks.get((cx - 1) + "");
        Chunk rightChunk = chunks.get((cx + 1) + "");

        int regionWidth = EnginePanel.CHUNK_WIDTH + 2;
        TileType[][] blockRegion = new TileType[EnginePanel.CHUNK_HEIGHT][regionWidth];
        byte[][] lightRegion = new byte[EnginePanel.CHUNK_HEIGHT][regionWidth];

        for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
            if (leftChunk != null) {
                blockRegion[row][0] = leftChunk.blocks[row][EnginePanel.CHUNK_WIDTH - 1];
                lightRegion[row][0] = leftChunk.lightMap[row][EnginePanel.CHUNK_WIDTH - 1];
            } else {
                blockRegion[row][0] = TileType.STONE;
            }

            if (rightChunk != null) {
                blockRegion[row][regionWidth - 1] = rightChunk.blocks[row][0];
                lightRegion[row][regionWidth - 1] = rightChunk.lightMap[row][0];
            } else {
                blockRegion[row][regionWidth - 1] = TileType.STONE;
            }

            System.arraycopy(mainChunk.blocks[row], 0, blockRegion[row], 1, EnginePanel.CHUNK_WIDTH);
        }

        for (int col = 0; col < regionWidth; col++) {
            for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
                if (blockRegion[row][col] != null && !blockRegion[row][col].isSolid()) {
                    if (lightRegion[row][col] < MAX_LIGHT_LEVEL) {
                        lightRegion[row][col] = MAX_LIGHT_LEVEL;
                    }
                } else {
                    break;
                }
            }
        }

        Queue<Point> lightQueue = new LinkedList<>();
        for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
            for (int col = 0; col < regionWidth; col++) {
                if (lightRegion[row][col] > 0) {
                    lightQueue.add(new Point(col, row));
                }
            }
        }

        propagateLight(blockRegion, lightRegion, lightQueue);

        for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
            System.arraycopy(lightRegion[row], 1, mainChunk.lightMap[row], 0, EnginePanel.CHUNK_WIDTH);
        }
    }

    /**
     * Spreads light from all sources in a queue through a region using BFS.
     * @param blocks The block data for the region.
     * @param lightMap The light map to modify.
     * @param queue The queue of initial light sources.
     */
    private void propagateLight(TileType[][] blocks, byte[][] lightMap, Queue<Point> queue) {
        int width = blocks[0].length;
        int height = blocks.length;

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int x = p.x;
            int y = p.y;

            int currentLightLevel = lightMap[y][x];
            int nextLightLevel = currentLightLevel - 1;

            if (nextLightLevel <= 0) {
                continue;
            }

            int[] dx = {0, 0, 1, -1};
            int[] dy = {1, -1, 0, 0};

            for (int i = 0; i < 4; i++) {
                int nx = x + dx[i];
                int ny = y + dy[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if (blocks[ny][nx] != null && lightMap[ny][nx] < nextLightLevel) {
                        if (blocks[ny][nx].isSolid()) {
                            if (lightMap[ny][nx] < nextLightLevel - 2) {
                                lightMap[ny][nx] = (byte) (nextLightLevel - 2);
                                queue.add(new Point(nx, ny));
                            }
                        } else {
                            lightMap[ny][nx] = (byte) nextLightLevel;
                            queue.add(new Point(nx, ny));
                        }
                    }
                }
            }
        }
    }

    private int countStoneNeighbors(TileType[][] blocks, int row, int col) {
        int count = 0;

        for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {

                if (x == 0 && y == 0) {
                    continue;
                }

                int nx = col + x;
                int ny = row + y;

                if (nx >= 0 && nx < EnginePanel.CHUNK_WIDTH && ny >= 0 && ny < EnginePanel.CHUNK_HEIGHT) {
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

}
