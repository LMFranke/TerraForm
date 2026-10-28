package br.lucasfranke.world.light;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.block.FurnaceBlock;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.Chunk;
import br.lucasfranke.world.World;

public class LightingEngine {

    public static final int MAX_LIGHT_LEVEL = 15;
    public static final int MIN_NIGHT_LIGHT_LEVEL = 2;

    private static final int[] DIR_X = {0, 0, -1, 1};
    private static final int[] DIR_Y = {-1, 1, 0, 0};

    private final World world;
    private int sunLightLevel = MAX_LIGHT_LEVEL;

    public LightingEngine(World world) {
        this.world = world;
    }

    public int getSunLightLevel() {
        return sunLightLevel;
    }

    public void setSunLightLevel(int newLevel, int centerChunkX) {
        int clamped = Math.clamp(newLevel, MIN_NIGHT_LIGHT_LEVEL, MAX_LIGHT_LEVEL);
        if (this.sunLightLevel != clamped) {
            this.sunLightLevel = clamped;
            for (int cx = centerChunkX - 2; cx <= centerChunkX + 2; cx++) {
                if (world.isChunkLoaded(cx)) {
                    calculateLightForChunk(cx);
                }
            }
        }
    }

    public byte getStaticLight(int tileX, int tileY) {
        if (!world.isValidY(tileY)) return (byte) sunLightLevel;
        Chunk c = world.getChunkByTileX(tileX);
        if (c == null) return 0;
        int localX = Math.floorMod(tileX, EnginePanel.CHUNK_WIDTH);
        return c.lightMap[tileY][localX];
    }

    public void calculateLightForChunkAndNeighbors(int cx) {
        calculateLightForChunk(cx);
        if (world.isChunkLoaded(cx + 1)) {
            calculateLightForChunk(cx + 1);
        }
        if (world.isChunkLoaded(cx - 1)) {
            calculateLightForChunk(cx - 1);
        }
    }

    public void calculateLightForChunk(int cx) {
        Chunk mainChunk = world.getLoadedChunk(cx);
        if (mainChunk == null) return;
        mainChunk.lastSunLightLevel = this.sunLightLevel;

        Chunk leftChunk = world.getLoadedChunk(cx - 1);
        Chunk rightChunk = world.getLoadedChunk(cx + 1);

        int cw = EnginePanel.CHUNK_WIDTH;
        int regionWidth = cw * 3;
        int height = EnginePanel.CHUNK_HEIGHT;

        TileType[][] blockRegion = new TileType[height][regionWidth];
        byte[][] lightRegion = new byte[height][regionWidth];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < cw; col++) {
                blockRegion[row][col] = (leftChunk != null) ? leftChunk.blocks[row][col] : TileType.STONE;
                blockRegion[row][col + cw] = mainChunk.blocks[row][col];
                blockRegion[row][col + cw * 2] = (rightChunk != null) ? rightChunk.blocks[row][col] : TileType.STONE;
            }
        }

        for (int col = 0; col < regionWidth; col++) {
            int currentSunLight = sunLightLevel;
            for (int row = 0; row < height; row++) {
                TileType tile = blockRegion[row][col];
                if (tile != null && !tile.isSolid()) {
                    if (tile == TileType.OAK_LEAVES) {
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

        int[] lightQueue = new int[height * regionWidth * 2];
        int head = 0;
        int tail = 0;

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < regionWidth; col++) {
                TileType tile = blockRegion[row][col];
                if (tile != null && tile.getLightEmission() > 0) {
                    int emission = tile.getLightEmission();
                    if (tile == TileType.FURNACE) {
                        Chunk targetChunk = (col < cw) ? leftChunk : (col < cw * 2 ? mainChunk : rightChunk);
                        FunctionalBlock fb = (targetChunk != null) ? targetChunk.getFunctionalBlock(col % cw, row) : null;
                        if (!(fb instanceof FurnaceBlock furnace && furnace.isBurning())) {
                            emission = 0;
                        }
                    }
                    if (lightRegion[row][col] < emission) {
                        lightRegion[row][col] = (byte) emission;
                    }
                }
                if (lightRegion[row][col] > 0) {
                    lightQueue[tail++] = (row << 8) | col;
                }
            }
        }

        propagateLight(blockRegion, lightRegion, lightQueue, head, tail);

        for (int row = 0; row < height; row++) {
            System.arraycopy(lightRegion[row], cw, mainChunk.lightMap[row], 0, cw);
        }
    }

    private void propagateLight(TileType[][] blocks, byte[][] lightMap, int[] queue, int head, int tail) {
        int width = blocks[0].length;
        int height = blocks.length;

        while (head < tail) {
            int packed = queue[head++];
            int y = packed >>> 8;
            int x = packed & 0xFF;

            int nextLightLevel = lightMap[y][x] - 1;
            if (nextLightLevel <= 0) {
                continue;
            }

            for (int i = 0; i < 4; i++) {
                int nx = x + DIR_X[i];
                int ny = y + DIR_Y[i];

                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    TileType neighbor = blocks[ny][nx];
                    if (neighbor != null && lightMap[ny][nx] < nextLightLevel) {
                        if (neighbor.isSolid()) {
                            int solidLight = nextLightLevel - 2;
                            if (lightMap[ny][nx] < solidLight) {
                                lightMap[ny][nx] = (byte) solidLight;
                                if (tail < queue.length) {
                                    queue[tail++] = (ny << 8) | nx;
                                }
                            }
                        } else {
                            lightMap[ny][nx] = (byte) nextLightLevel;
                            if (tail < queue.length) {
                                queue[tail++] = (ny << 8) | nx;
                            }
                        }
                    }
                }
            }
        }
    }
}
