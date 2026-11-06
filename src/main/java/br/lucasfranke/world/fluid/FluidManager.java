package br.lucasfranke.world.fluid;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.Chunk;
import br.lucasfranke.world.World;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

public class FluidManager {

    public static final int MAX_WATER_LEVEL = 6;
    private static final int FLUID_TICK_INTERVAL = 14;
    private static final int MAX_UPDATES_PER_STEP = 512;

    private final World world;
    private Set<Long> activeTiles = new HashSet<>();
    private Set<Long> nextActiveTiles = new HashSet<>();
    private int tickCounter = 0;
    private boolean updatingFluid = false;

    public FluidManager(World world) {
        this.world = world;
    }

    public boolean isUpdatingFluid() {
        return updatingFluid;
    }

    public void onBlockChanged(int tileX, int tileY, TileType newType) {
        if (updatingFluid) {
            return;
        }
        if (newType != TileType.WATER && world.getWaterLevel(tileX, tileY) > 0) {
            Chunk c = world.getLoadedChunk(world.toChunkX(tileX));
            if (c != null) {
                c.setWaterData(tileX, tileY, 0, false, false);
            }
        }
        wakeNeighbors(tileX, tileY);
    }

    public void wakeNeighbors(int tileX, int tileY) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = tileX + dx;
                int ny = tileY + dy;
                if (world.isValidY(ny)) {
                    activeTiles.add(packPos(nx, ny));
                }
            }
        }
    }

    public boolean placeWaterSource(int tileX, int tileY) {
        if (!world.isValidY(tileY)) return false;
        TileType current = world.getBlock(tileX, tileY);
        if (current != TileType.AIR && current != TileType.WATER && !isWashable(current)) {
            return false;
        }
        updatingFluid = true;
        try {
            washBlockIfFragile(tileX, tileY, current);
            Chunk c = world.getChunkByTileX(tileX);
            if (c != null) {
                c.setBlock(tileX, tileY, TileType.WATER);
                c.setWaterData(tileX, tileY, MAX_WATER_LEVEL, true, false);
            }
        } finally {
            updatingFluid = false;
        }
        wakeNeighbors(tileX, tileY);
        world.getLighting().calculateLightForChunkAndNeighbors(world.toChunkX(tileX));
        return true;
    }

    public boolean collectWaterSource(int tileX, int tileY) {
        if (!world.isValidY(tileY)) return false;
        int targetX = tileX;
        int targetY = tileY;

        if (!world.isWaterSource(targetX, targetY)) {
            boolean found = false;
            int[][] dirs = {{0, 0}, {0, -1}, {-1, 0}, {1, 0}, {0, 1}};
            for (int[] d : dirs) {
                if (world.isWaterSource(tileX + d[0], tileY + d[1])) {
                    targetX = tileX + d[0];
                    targetY = tileY + d[1];
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }

        while (world.isValidY(targetY - 1) && world.isWaterSource(targetX, targetY - 1)) {
            targetY--;
        }

        updatingFluid = true;
        try {
            Chunk c = world.getChunkByTileX(targetX);
            if (c != null) {
                c.setWaterData(targetX, targetY, 0, false, false);
                c.setBlock(targetX, targetY, TileType.AIR);
            }
        } finally {
            updatingFluid = false;
        }
        wakeNeighbors(targetX, targetY);
        world.getLighting().calculateLightForChunkAndNeighbors(world.toChunkX(targetX));
        return true;
    }

    public void update(int centerChunkX, int radius) {
        if (activeTiles.isEmpty()) {
            return;
        }
        tickCounter++;
        if (tickCounter < FLUID_TICK_INTERVAL) {
            return;
        }
        tickCounter = 0;

        int minChunkX = centerChunkX - radius;
        int maxChunkX = centerChunkX + radius;

        Set<Long> currentBatch = activeTiles;
        activeTiles = nextActiveTiles;
        activeTiles.clear();

        Set<Integer> chunksToRelight = null;
        int processed = 0;

        updatingFluid = true;
        try {
            for (long packed : currentBatch) {
                int x = unpackX(packed);
                int y = unpackY(packed);
                if (!world.isValidY(y)) continue;

                int cx = world.toChunkX(x);
                if (cx < minChunkX || cx > maxChunkX || !world.isChunkLoaded(cx)) {
                    activeTiles.add(packed);
                    continue;
                }

                if (processed >= MAX_UPDATES_PER_STEP) {
                    activeTiles.add(packed);
                    continue;
                }
                processed++;

                if (stepTile(x, y)) {
                    wakeNeighborsInto(x, y, activeTiles);
                    if (chunksToRelight == null) {
                        chunksToRelight = new HashSet<>();
                    }
                    chunksToRelight.add(cx);
                }
            }
        } finally {
            updatingFluid = false;
        }

        nextActiveTiles = currentBatch;
        nextActiveTiles.clear();

        if (chunksToRelight != null) {
            for (int cx : chunksToRelight) {
                world.getLighting().calculateLightForChunk(cx);
            }
        }
    }

    private boolean stepTile(int x, int y) {
        TileType fg = world.getBlock(x, y);
        if (fg != TileType.AIR && fg != TileType.WATER && !isWashable(fg)) {
            return false;
        }

        int curLevel = world.getWaterLevel(x, y);
        boolean curSource = world.isWaterSource(x, y);
        boolean curFalling = world.isWaterFalling(x, y);

        if (curSource) {
            TryPushSpread(x, y, MAX_WATER_LEVEL);
            return false;
        }

        boolean hasLeftSource = world.isWaterSource(x - 1, y);
        boolean hasRightSource = world.isWaterSource(x + 1, y);
        boolean hasBottomSolidOrWater = world.isSolid(x, y + 1) || world.getWaterLevel(x, y + 1) > 0;

        if ((hasLeftSource && hasRightSource) && hasBottomSolidOrWater) {
            setWaterState(x, y, MAX_WATER_LEVEL, true, false);
            return true;
        }

        int targetLevel = 0;
        boolean targetFalling = false;

        int aboveLevel = world.getWaterLevel(x, y - 1);
        if (aboveLevel > 0) {
            targetLevel = MAX_WATER_LEVEL;
            targetFalling = true;
        } else {
            int maxHorizontalSupply = 0;
            if (canSpreadHorizontallyFrom(x - 1, y)) {
                maxHorizontalSupply = Math.max(maxHorizontalSupply, world.getWaterLevel(x - 1, y) - 1);
            }
            if (canSpreadHorizontallyFrom(x + 1, y)) {
                maxHorizontalSupply = Math.max(maxHorizontalSupply, world.getWaterLevel(x + 1, y) - 1);
            }
            targetLevel = maxHorizontalSupply;
        }

        int newLevel = curLevel;
        boolean newFalling = targetFalling;

        if (targetLevel > curLevel) {
            newLevel = targetLevel;
        } else if (targetLevel < curLevel) {
            newLevel = curLevel - 1;
            if (newLevel <= 0) {
                newLevel = 0;
                newFalling = false;
            }
        }

        boolean changed = (newLevel != curLevel) || (newFalling != curFalling);
        if (changed) {
            setWaterState(x, y, newLevel, false, newFalling);
        }

        if (newLevel > 0) {
            TryPushSpread(x, y, newLevel);
        }

        return changed;
    }

    private void TryPushSpread(int x, int y, int myLevel) {
        if (world.isValidY(y + 1)) {
            TileType below = world.getBlock(x, y + 1);
            if ((below == TileType.AIR || below == TileType.WATER || isWashable(below))
                    && (world.getWaterLevel(x, y + 1) < MAX_WATER_LEVEL || !world.isWaterFalling(x, y + 1))) {
                if (!world.isWaterSource(x, y + 1)) {
                    activeTiles.add(packPos(x, y + 1));
                }
            }
        }
        if (myLevel > 1 && hasBottomSupportForLateralSpread(x, y)) {
            checkLateralNeighborWake(x - 1, y, myLevel - 1);
            checkLateralNeighborWake(x + 1, y, myLevel - 1);
        }
    }

    private void checkLateralNeighborWake(int nx, int y, int offeredLevel) {
        TileType neighbor = world.getBlock(nx, y);
        if (neighbor == TileType.AIR || isWashable(neighbor)
                || (neighbor == TileType.WATER && world.getWaterLevel(nx, y) < offeredLevel)) {
            activeTiles.add(packPos(nx, y));
        }
    }

    private boolean canSpreadHorizontallyFrom(int nx, int y) {
        int neighborLevel = world.getWaterLevel(nx, y);
        if (neighborLevel <= 1) {
            return false;
        }
        return hasBottomSupportForLateralSpread(nx, y);
    }

    private boolean hasBottomSupportForLateralSpread(int x, int y) {
        if (!world.isValidY(y + 1)) {
            return true;
        }
        if (world.isSolid(x, y + 1)) {
            return true;
        }
        return world.getWaterLevel(x, y + 1) == MAX_WATER_LEVEL && !world.isWaterFalling(x, y + 1);
    }

    private void setWaterState(int x, int y, int level, boolean source, boolean falling) {
        Chunk c = world.getChunkByTileX(x);
        if (c == null) return;

        if (level <= 0) {
            c.setWaterData(x, y, 0, false, false);
            if (c.getBlock(x, y) == TileType.WATER) {
                c.setBlock(x, y, TileType.AIR);
            }
        } else {
            TileType current = c.getBlock(x, y);
            washBlockIfFragile(x, y, current);
            c.setBlock(x, y, TileType.WATER);
            c.setWaterData(x, y, level, source, falling);
        }
    }

    private boolean isWashable(TileType type) {
        return type == TileType.TORCH || type == TileType.OAK_SAPLING;
    }

    private void washBlockIfFragile(int x, int y, TileType current) {
        if (isWashable(current)) {
            Chunk c = world.getChunkByTileX(x);
            if (c != null) {
                c.setBlock(x, y, TileType.AIR);
            }
            if (EnginePanel.worldManager != null && EnginePanel.worldManager.entityManager != null) {
                EnginePanel.worldManager.entityManager.spawnTileDrop(x, y, new Item(current, 1));
            }
        }
    }

    public double[] getWaterFlowForce(Rectangle bounds) {
        int tileSize = EnginePanel.tileSquare;
        int leftTile = Math.floorDiv(bounds.x, tileSize);
        int rightTile = Math.floorDiv(bounds.x + bounds.width - 1, tileSize);
        int topTile = Math.floorDiv(bounds.y, tileSize);
        int bottomTile = Math.floorDiv(bounds.y + bounds.height - 1, tileSize);

        double totalFlowX = 0.0;
        double totalFlowY = 0.0;
        int samples = 0;

        for (int ty = topTile; ty <= bottomTile; ty++) {
            if (!world.isValidY(ty)) continue;
            for (int tx = leftTile; tx <= rightTile; tx++) {
                int level = world.getWaterLevel(tx, ty);
                if (level <= 0) continue;

                double localDirX = calculateTileFlowDirX(tx, ty, level);
                totalFlowX += localDirX;
                if (world.isWaterFalling(tx, ty) || world.getWaterLevel(tx, ty - 1) > 0) {
                    totalFlowY += 1.0;
                }
                samples++;
            }
        }

        if (samples == 0) {
            return new double[]{0.0, 0.0};
        }
        return new double[]{
                Math.clamp(totalFlowX / samples, -1.8, 1.8),
                Math.clamp(totalFlowY / samples, 0.0, 1.8)
        };
    }

    public int calculateTileFlowDirX(int tx, int ty, int level) {
        if (level <= 0) return 0;
        int pushRight = 0;

        int leftLevel = world.getWaterLevel(tx - 1, ty);
        if (leftLevel > 0) {
            pushRight += (leftLevel - level);
        } else if (!world.isSolid(tx - 1, ty)) {
            pushRight -= 2;
        }

        int rightLevel = world.getWaterLevel(tx + 1, ty);
        if (rightLevel > 0) {
            pushRight += (level - rightLevel);
        } else if (!world.isSolid(tx + 1, ty)) {
            pushRight += 2;
        }

        return Integer.compare(pushRight, 0);
    }

    public int getWaterSurfaceWorldY(int tileX, int tileY) {
        int tileSize = EnginePanel.tileSquare;
        int level = world.getWaterLevel(tileX, tileY);
        if (level <= 0) {
            return (tileY + 1) * tileSize;
        }
        if (world.isWaterFalling(tileX, tileY) || world.getWaterLevel(tileX, tileY - 1) > 0) {
            return tileY * tileSize;
        }
        int waterPixelHeight = Math.round((level / (float) MAX_WATER_LEVEL) * tileSize);
        return (tileY + 1) * tileSize - waterPixelHeight;
    }

    private void wakeNeighborsInto(int tileX, int tileY, Set<Long> targetSet) {
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = tileX + dx;
                int ny = tileY + dy;
                if (world.isValidY(ny)) {
                    targetSet.add(packPos(nx, ny));
                }
            }
        }
    }

    private static long packPos(int x, int y) {
        return (((long) x) << 32) | (y & 0xFFFFFFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackY(long packed) {
        return (int) packed;
    }
}