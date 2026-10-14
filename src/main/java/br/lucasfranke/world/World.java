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
    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int MIN_NIGHT_LIGHT_LEVEL = 2;

    private int sunLightLevel = MAX_LIGHT_LEVEL;

    public World(long seed) {
        this.seed = seed;
        this.terrainNoise = new Noise1D(seed);
        this.caveNoise = new Noise2D(seed);
    }

    public int getSunLightLevel() {
        return sunLightLevel;
    }

    public void setSunLightLevel(int newLevel, int centerChunkX) {
        int clamped = Math.clamp(newLevel, MIN_NIGHT_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
        if (this.sunLightLevel != clamped) {
            this.sunLightLevel = clamped;
            for (int cx = centerChunkX - 2; cx <= centerChunkX + 2; cx++) {
                if (chunks.containsKey(cx + "")) {
                    calculateLightForChunk(cx);
                }
            }
        }
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
        } else {
            Chunk c = chunks.get(key);
            if (c.lastSunLightLevel != this.sunLightLevel) {
                calculateLightForChunk(cx);
            }
        }
        return chunks.get(key);
    }

    private Chunk generateChunk(int cx) {
        TileType[][] blocks = new TileType[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
        TileType[][] backgroundBlocks = new TileType[EnginePanel.CHUNK_HEIGHT][EnginePanel.CHUNK_WIDTH];
        int worldXStart = cx * EnginePanel.CHUNK_WIDTH;
        int worldYStart = 10;

        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
                int worldX = worldXStart + col;
                int worldY = worldYStart + row;

                int groundHeight = (int) (terrainNoise.sample(worldX * 0.05) * 15 + 40);
                int stoneHeight = groundHeight + 5;
                stoneHeight += (int) (terrainNoise.sample(worldX * 0.1) * 3);

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

        for (int row = EnginePanel.MAX_WORLD_HEIGHT - 1; row >= EnginePanel.MIN_WORLD_HEIGHT; row--) {
            for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
                int worldX = worldXStart + col;
                int worldY = worldYStart + row;

                if (blocks[row][col] == TileType.STONE) {
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

        return new Chunk(cx, newBlocks, backgroundBlocks);
    }

    public void calculateLightForChunkAndNeighbors(int cx) {
        calculateLightForChunk(cx);
        if (chunks.containsKey((cx + 1) + "")) {
            calculateLightForChunk(cx + 1);
        }
        if (chunks.containsKey((cx - 1) + "")) {
            calculateLightForChunk(cx - 1);
        }
    }

    public void calculateLightForChunk(int cx) {
        Chunk mainChunk = chunks.get(cx + "");
        if (mainChunk == null) {
            return;
        }
        Chunk leftChunk = chunks.get((cx - 1) + "");
        Chunk rightChunk = chunks.get((cx + 1) + "");

        int cw = EnginePanel.CHUNK_WIDTH;
        int regionWidth = cw * 3;
        TileType[][] blockRegion = new TileType[EnginePanel.CHUNK_HEIGHT][regionWidth];
        byte[][] lightRegion = new byte[EnginePanel.CHUNK_HEIGHT][regionWidth];

        for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
            for (int col = 0; col < cw; col++) {
                blockRegion[row][col] = (leftChunk != null) ? leftChunk.blocks[row][col] : TileType.STONE;
                blockRegion[row][col + cw] = mainChunk.blocks[row][col];
                blockRegion[row][col + cw * 2] = (rightChunk != null) ? rightChunk.blocks[row][col] : TileType.STONE;
            }
        }

        for (int col = 0; col < regionWidth; col++) {
            for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
                TileType tile = blockRegion[row][col];
                if (tile != null && !tile.isSolid()) {
                    if (lightRegion[row][col] < sunLightLevel) {
                        lightRegion[row][col] = (byte) sunLightLevel;
                    }
                } else {
                    break;
                }
            }
        }

        Queue<Point> lightQueue = new LinkedList<>();
        for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
            for (int col = 0; col < regionWidth; col++) {
                TileType tile = blockRegion[row][col];
                if (tile != null && tile.getLightEmission() > 0) {
                    if (lightRegion[row][col] < tile.getLightEmission()) {
                        lightRegion[row][col] = (byte) tile.getLightEmission();
                    }
                }
                if (lightRegion[row][col] > 0) {
                    lightQueue.add(new Point(col, row));
                }
            }
        }

        propagateLight(blockRegion, lightRegion, lightQueue);

        for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
            System.arraycopy(lightRegion[row], cw, mainChunk.lightMap[row], 0, cw);
        }
        mainChunk.lastSunLightLevel = this.sunLightLevel;
    }

    private void propagateLight(TileType[][] blocks, byte[][] lightMap, Queue<Point> queue) {
        int width = blocks[0].length;
        int height = blocks.length;

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        while (!queue.isEmpty()) {
            Point p = queue.poll();
            int x = p.x;
            int y = p.y;
            int currentLightLevel = lightMap[y][x];
            int nextLightLevel = currentLightLevel - 1;

            if (nextLightLevel <= 0) {
                continue;
            }

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