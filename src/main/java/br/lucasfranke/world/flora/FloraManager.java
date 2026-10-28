package br.lucasfranke.world.flora;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.Chunk;
import br.lucasfranke.world.World;

import java.awt.Point;
import java.util.*;

public class FloraManager {

    private static final int MAX_LEAF_TRUNK_DISTANCE = 4;
    private static final int LEAF_DECAY_SCAN_INTERVAL = 10;
    private static final int MIN_LEAF_DECAY_TICKS = 45;  // ~0.75s 60 UPS
    private static final int MAX_LEAF_DECAY_TICKS = 210; // ~3.5s  60 UPS
    private static final double LEAF_DROP_STICK_CHANCE = 0.12;
    private static final double LEAF_DROP_SAPLING_CHANCE = 0.08;
    private static final int MIN_SAPLING_GROW_TICKS = 600;  // 10s em 60 UPS
    private static final int MAX_SAPLING_GROW_TICKS = 1500; // 25s em 60 UPS

    private static final int BFS_RADIUS = MAX_LEAF_TRUNK_DISTANCE;
    private static final int BFS_GRID_SIZE = BFS_RADIUS * 2 + 1;
    private static final int BFS_MAX_QUEUE = BFS_GRID_SIZE * BFS_GRID_SIZE;

    private static final int[] DIR_X = {0, 0, -1, 1};
    private static final int[] DIR_Y = {-1, 1, 0, 0};

    private final World world;
    private final Map<Point, Integer> growingSaplings = new HashMap<>();
    private final Map<Point, Integer> decayingLeaves = new HashMap<>();
    private final Random random = new Random();

    private int leafDecayTickCounter = 0;
    private boolean leafScanDirty = true;
    private int lastScannedCenterChunkX = Integer.MIN_VALUE;

    private final int[] bfsQueueX = new int[BFS_MAX_QUEUE];
    private final int[] bfsQueueY = new int[BFS_MAX_QUEUE];
    private final int[] bfsQueueDist = new int[BFS_MAX_QUEUE];
    private final int[][] bfsVisitedStamp = new int[BFS_GRID_SIZE][BFS_GRID_SIZE];
    private int bfsCurrentStamp = 0;

    public FloraManager(World world) {
        this.world = world;
    }

    public void markScanDirty() {
        this.leafScanDirty = true;
    }

    public void onForegroundBlockChanged(int tileX, int tileY, TileType type) {
        this.leafScanDirty = true;
        Point pos = new Point(tileX, tileY);
        if (type == TileType.OAK_SAPLING) {
            int growDelay = MIN_SAPLING_GROW_TICKS
                    + random.nextInt(MAX_SAPLING_GROW_TICKS - MIN_SAPLING_GROW_TICKS + 1);
            growingSaplings.put(pos, growDelay);
        } else if (!growingSaplings.isEmpty()) {
            growingSaplings.remove(pos);
        }
    }

    public void updateNaturalLeaves(int centerChunkX, int radius) {
        leafDecayTickCounter++;
        if (leafDecayTickCounter >= LEAF_DECAY_SCAN_INTERVAL) {
            leafDecayTickCounter = 0;
            if (leafScanDirty || centerChunkX != lastScannedCenterChunkX) {
                leafScanDirty = false;
                lastScannedCenterChunkX = centerChunkX;
                scanUnsupportedNaturalLeaves(centerChunkX, radius);
            }
        }

        if (decayingLeaves.isEmpty()) {
            return;
        }

        Set<Integer> chunksToRelight = null;
        Iterator<Map.Entry<Point, Integer>> it = decayingLeaves.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Point, Integer> entry = it.next();
            Point pos = entry.getKey();
            Chunk chunk = world.getLoadedChunk(world.toChunkX(pos.x));
            int localX = Math.floorMod(pos.x, EnginePanel.CHUNK_WIDTH);

            if (chunk == null
                    || !world.isValidY(pos.y)
                    || chunk.blocks[pos.y][localX] != TileType.OAK_LEAVES
                    || !chunk.naturalTreeTiles[pos.y][localX]) {
                it.remove();
                continue;
            }

            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                it.remove();
                if (!isLeafConnectedToTrunk(pos.x, pos.y)) {
                    world.setBlock(pos.x, pos.y, TileType.AIR);
                    rollLeafDrops(EnginePanel.player);
                    int cx = world.toChunkX(pos.x);
                    if (chunksToRelight == null) {
                        chunksToRelight = new HashSet<>();
                    }
                    chunksToRelight.add(cx - 1);
                    chunksToRelight.add(cx);
                    chunksToRelight.add(cx + 1);
                }
            } else {
                entry.setValue(ticksLeft);
            }
        }

        if (chunksToRelight != null) {
            for (int cx : chunksToRelight) {
                world.getLighting().calculateLightForChunk(cx);
            }
        }
    }

    public void rollLeafDrops(Player player) {
        if (player == null) {
            return;
        }
        if (random.nextDouble() < LEAF_DROP_STICK_CHANCE) {
            int stickQty = 1 + random.nextInt(2);
            player.addItem(TileType.STICK, stickQty);
        }
        if (random.nextDouble() < LEAF_DROP_SAPLING_CHANCE) {
            player.addItem(TileType.OAK_SAPLING, 1);
        }
    }

    private void scanUnsupportedNaturalLeaves(int centerChunkX, int radius) {
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            Chunk chunk = world.getLoadedChunk(cx);
            if (chunk == null) {
                continue;
            }
            int worldXStart = cx * EnginePanel.CHUNK_WIDTH;
            for (int row = 0; row < EnginePanel.CHUNK_HEIGHT; row++) {
                TileType[] rowBlocks = chunk.blocks[row];
                boolean[] rowNatural = chunk.naturalTreeTiles[row];
                for (int col = 0; col < EnginePanel.CHUNK_WIDTH; col++) {
                    if (rowBlocks[col] == TileType.OAK_LEAVES && rowNatural[col]) {
                        int worldX = worldXStart + col;
                        boolean connected = isLeafConnectedToTrunk(worldX, row);
                        if (!connected) {
                            Point pos = new Point(worldX, row);
                            if (!decayingLeaves.containsKey(pos)) {
                                int delay = MIN_LEAF_DECAY_TICKS
                                        + random.nextInt(MAX_LEAF_DECAY_TICKS - MIN_LEAF_DECAY_TICKS + 1);
                                decayingLeaves.put(pos, delay);
                            }
                        } else if (!decayingLeaves.isEmpty()) {
                            decayingLeaves.remove(new Point(worldX, row));
                        }
                    }
                }
            }
        }
    }

    private boolean isLeafConnectedToTrunk(int startX, int startY) {
        if (isTrunkAtFast(startX, startY)) {
            return true;
        }
        bfsCurrentStamp++;
        if (bfsCurrentStamp == Integer.MAX_VALUE) {
            for (int[] row : bfsVisitedStamp) {
                Arrays.fill(row, 0);
            }
            bfsCurrentStamp = 1;
        }

        int head = 0;
        int tail = 0;
        bfsQueueX[tail] = startX;
        bfsQueueY[tail] = startY;
        bfsQueueDist[tail] = 0;
        tail++;
        bfsVisitedStamp[BFS_RADIUS][BFS_RADIUS] = bfsCurrentStamp;

        int cachedChunkX = world.toChunkX(startX);
        Chunk cachedChunk = world.getLoadedChunk(cachedChunkX);

        while (head < tail) {
            int cx = bfsQueueX[head];
            int cy = bfsQueueY[head];
            int dist = bfsQueueDist[head];
            head++;

            if (dist >= MAX_LEAF_TRUNK_DISTANCE) {
                continue;
            }

            for (int i = 0; i < 4; i++) {
                int nx = cx + DIR_X[i];
                int ny = cy + DIR_Y[i];
                if (!world.isValidY(ny)) {
                    continue;
                }
                int targetChunkX = world.toChunkX(nx);
                Chunk targetChunk;
                if (targetChunkX == cachedChunkX) {
                    targetChunk = cachedChunk;
                } else {
                    targetChunk = world.getLoadedChunk(targetChunkX);
                    cachedChunkX = targetChunkX;
                    cachedChunk = targetChunk;
                }
                if (targetChunk == null) {
                    continue;
                }
                int localX = Math.floorMod(nx, EnginePanel.CHUNK_WIDTH);
                if (targetChunk.backgroundBlocks[ny][localX] == TileType.BACKGROUND_WOOD
                        || targetChunk.blocks[ny][localX] == TileType.OAK_WOOD) {
                    return true;
                }
                int gridX = nx - startX + BFS_RADIUS;
                int gridY = ny - startY + BFS_RADIUS;
                if (bfsVisitedStamp[gridY][gridX] != bfsCurrentStamp
                        && targetChunk.blocks[ny][localX] == TileType.OAK_LEAVES) {
                    bfsVisitedStamp[gridY][gridX] = bfsCurrentStamp;
                    bfsQueueX[tail] = nx;
                    bfsQueueY[tail] = ny;
                    bfsQueueDist[tail] = dist + 1;
                    tail++;
                }
            }
        }
        return false;
    }

    private boolean isTrunkAtFast(int tileX, int tileY) {
        if (!world.isValidY(tileY)) return false;
        Chunk c = world.getLoadedChunk(world.toChunkX(tileX));
        if (c == null) return false;
        int localX = Math.floorMod(tileX, EnginePanel.CHUNK_WIDTH);
        return c.backgroundBlocks[tileY][localX] == TileType.BACKGROUND_WOOD
                || c.blocks[tileY][localX] == TileType.OAK_WOOD;
    }

    public void updateSaplings() {
        if (growingSaplings.isEmpty()) {
            return;
        }
        List<Point> readyToGrow = null;
        Iterator<Map.Entry<Point, Integer>> it = growingSaplings.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Point, Integer> entry = it.next();
            Point pos = entry.getKey();
            if (world.getBlock(pos.x, pos.y) != TileType.OAK_SAPLING) {
                it.remove();
                continue;
            }
            if (world.getLighting().getStaticLight(pos.x, pos.y) < 9) {
                continue;
            }
            int ticksLeft = entry.getValue() - 1;
            if (ticksLeft <= 0) {
                if (readyToGrow == null) {
                    readyToGrow = new ArrayList<>();
                }
                readyToGrow.add(pos);
            } else {
                entry.setValue(ticksLeft);
            }
        }

        if (readyToGrow != null) {
            for (Point pos : readyToGrow) {
                int trunkHeight = 4 + random.nextInt(3);
                if (TreeFeature.tryGrowInWorld(world, pos.x, pos.y, trunkHeight)) {
                    growingSaplings.remove(pos);
                } else {
                    growingSaplings.put(pos, 180);
                }
            }
        }
    }
}
