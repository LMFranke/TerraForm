package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class World {

    private final Map<Integer, Chunk> chunks = new HashMap<>();
    private final long seed;
    private final WorldGenerator generator;

    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int MIN_NIGHT_LIGHT_LEVEL = 2;
    private int sunLightLevel = MAX_LIGHT_LEVEL;

    public World(long seed) {
        this.seed = seed;
        this.generator = new WorldGenerator(seed);
    }

    public long getSeed() {
        return seed;
    }

    public boolean isValidY(int tileY) {
        return tileY >= EnginePanel.MIN_WORLD_HEIGHT && tileY < EnginePanel.CHUNK_HEIGHT;
    }

    public int toChunkX(int tileX) {
        return Math.floorDiv(tileX, EnginePanel.CHUNK_WIDTH);
    }

    public Chunk getChunkByTileX(int tileX) {
        return getChunk(toChunkX(tileX));
    }

    public TileType getBlock(int tileX, int tileY) {
        if (!isValidY(tileY)) return TileType.AIR;
        Chunk c = getChunkByTileX(tileX);
        if (c == null) return TileType.AIR;
        TileType t = c.getBlock(tileX, tileY);
        return t != null ? t : TileType.AIR;
    }

    public void setBlock(int tileX, int tileY, TileType type) {
        if (!isValidY(tileY)) return;
        Chunk c = getChunkByTileX(tileX);
        if (c != null) {
            c.setBlock(tileX, tileY, type);
        }
    }

    public TileType getBackgroundBlock(int tileX, int tileY) {
        if (!isValidY(tileY)) return TileType.AIR;
        Chunk c = getChunkByTileX(tileX);
        if (c == null) return TileType.AIR;
        TileType t = c.getBackgroundBlock(tileX, tileY);
        return t != null ? t : TileType.AIR;
    }

    public TileType getNaturalBackgroundBlock(int tileX, int tileY) {
        if (!isValidY(tileY)) return TileType.AIR;
        Chunk c = getChunkByTileX(tileX);
        if (c == null) return TileType.AIR;
        int localX = Math.floorMod(tileX, EnginePanel.CHUNK_WIDTH);
        TileType t = c.originalBackgroundBlocks[tileY][localX];
        return t != null ? t : TileType.AIR;
    }

    public void setBackgroundBlock(int tileX, int tileY, TileType type) {
        if (!isValidY(tileY)) return;
        Chunk c = getChunkByTileX(tileX);
        if (c != null) {
            c.setBackgroundBlock(tileX, tileY, type);
        }
    }

    public FunctionalBlock getFunctionalBlock(int tileX, int tileY) {
        if (!isValidY(tileY)) return null;
        Chunk c = getChunkByTileX(tileX);
        return c != null ? c.getFunctionalBlock(tileX, tileY) : null;
    }

    public byte getStaticLight(int tileX, int tileY) {
        if (!isValidY(tileY)) return (byte) sunLightLevel;
        Chunk c = getChunkByTileX(tileX);
        if (c == null) return 0;
        int localX = Math.floorMod(tileX, EnginePanel.CHUNK_WIDTH);
        return c.lightMap[tileY][localX];
    }

    public boolean isSolid(int tileX, int tileY) {
        return getBlock(tileX, tileY).isSolid();
    }

    public void updateActiveFunctionalBlocks(int centerChunkX, int radius) {
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            Chunk c = chunks.get(cx);
            if (c != null) {
                c.updateFunctionalBlocks();
            }
        }
    }

    public int getSunLightLevel() {
        return sunLightLevel;
    }

    public void setSunLightLevel(int newLevel, int centerChunkX) {
        int clamped = Math.clamp(newLevel, MIN_NIGHT_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
        if (this.sunLightLevel != clamped) {
            this.sunLightLevel = clamped;
            for (int cx = centerChunkX - 2; cx <= centerChunkX + 2; cx++) {
                if (chunks.containsKey(cx)) {
                    calculateLightForChunk(cx);
                }
            }
        }
    }

    public Chunk getChunk(int cx) {
        Chunk c = chunks.get(cx);
        if (c == null) {
            c = generator.generateChunk(cx);
            chunks.put(cx, c);
            calculateLightForChunk(cx);
            if (chunks.containsKey(cx - 1)) {
                calculateLightForChunk(cx - 1);
            }
            if (chunks.containsKey(cx + 1)) {
                calculateLightForChunk(cx + 1);
            }
        } else if (c.lastSunLightLevel != this.sunLightLevel) {
            calculateLightForChunk(cx);
        }
        return c;
    }

    public void calculateLightForChunkAndNeighbors(int cx) {
        calculateLightForChunk(cx);
        if (chunks.containsKey(cx + 1)) {
            calculateLightForChunk(cx + 1);
        }
        if (chunks.containsKey(cx - 1)) {
            calculateLightForChunk(cx - 1);
        }
    }

    public void calculateLightForChunk(int cx) {
        Chunk mainChunk = chunks.get(cx);
        if (mainChunk == null) return;

        Chunk leftChunk = chunks.get(cx - 1);
        Chunk rightChunk = chunks.get(cx + 1);

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
            int currentSunLight = sunLightLevel;
            for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
                TileType tile = blockRegion[row][col];
                if (tile != null && !tile.isSolid()) {
                    if (tile == TileType.LEAVES) {
                        currentSunLight = Math.max(MIN_NIGHT_LIGHT_LEVEL, currentSunLight - 1);
                    }
                    if (lightRegion[row][col] < currentSunLight) {
                        lightRegion[row][col] = (byte) currentSunLight;
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
            int nextLightLevel = lightMap[y][x] - 1;

            if (nextLightLevel <= 0) continue;

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
}