package br.lucasfranke.world.fluid;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.Chunk;

public final class WaterFeature {

    private WaterFeature() {
    }

    public static void generateSurfaceLake(Chunk chunk, int[] surfaceRows, int[] stoneStartRows, long seed) {
        if (chunk.cx == 625 || chunk.cx == 0) {
            return;
        }

        int hash = deterministicHash(seed, chunk.cx * 7919);
        if ((hash & 0xFF) >= 68) {
            return;
        }

        boolean isMediumLake = ((hash >>> 8) & 1) == 1;
        int lakeWidth = isMediumLake
                ? 8 + (Math.abs(hash >>> 9) % 4)
                : 5 + (Math.abs(hash >>> 9) % 3);

        int maxStartCol = EnginePanel.CHUNK_WIDTH - lakeWidth - 2;
        if (maxStartCol < 2) {
            return;
        }
        int startCol = 2 + (Math.abs(hash >>> 13) % (maxStartCol - 1));
        int endCol = startCol + lakeWidth - 1;

        int leftBankCol = startCol - 1;
        int rightBankCol = endCol + 1;

        int leftSurface = surfaceRows[leftBankCol];
        int rightSurface = surfaceRows[rightBankCol];
        if (Math.abs(leftSurface - rightSurface) > 4) {
            return;
        }

        int waterSurfaceRow = Math.max(leftSurface, rightSurface);
        int maxDepth = isMediumLake ? 4 : 3;
        if (waterSurfaceRow < 12 || waterSurfaceRow + maxDepth + 3 >= EnginePanel.CHUNK_HEIGHT) {
            return;
        }

        sealBankColumn(chunk, leftBankCol, waterSurfaceRow, waterSurfaceRow + maxDepth + 1, stoneStartRows[leftBankCol]);
        sealBankColumn(chunk, rightBankCol, waterSurfaceRow, waterSurfaceRow + maxDepth + 1, stoneStartRows[rightBankCol]);

        double center = (startCol + endCol) / 2.0;
        double halfSpan = Math.max(1.0, (endCol - startCol) / 2.0 + 0.5);

        for (int col = startCol; col <= endCol; col++) {
            double normDist = Math.abs(col - center) / halfSpan;
            int depth = Math.max(1, (int) Math.round(maxDepth * (1.0 - normDist * normDist * 0.75)));

            for (int r = Math.min(surfaceRows[col], waterSurfaceRow) - 1; r < waterSurfaceRow; r++) {
                if (r >= 0) {
                    chunk.blocks[r][col] = TileType.AIR;
                    chunk.backgroundBlocks[r][col] = TileType.AIR;
                    chunk.originalBackgroundBlocks[r][col] = TileType.AIR;
                }
            }

            int bottomWaterRow = waterSurfaceRow + depth - 1;
            for (int r = waterSurfaceRow; r <= bottomWaterRow; r++) {
                chunk.blocks[r][col] = TileType.WATER;
                chunk.setWaterData(col, r, FluidManager.MAX_WATER_LEVEL, true, false);
                TileType bg = (r >= stoneStartRows[col]) ? TileType.BACKGROUND_STONE : TileType.BACKGROUND_DIRT;
                chunk.backgroundBlocks[r][col] = bg;
                chunk.originalBackgroundBlocks[r][col] = bg;
            }

            for (int r = bottomWaterRow + 1; r <= bottomWaterRow + 2 && r < EnginePanel.CHUNK_HEIGHT; r++) {
                boolean isStoneDepth = r >= stoneStartRows[col];
                if (!chunk.blocks[r][col].isSolid()) {
                    chunk.blocks[r][col] = isStoneDepth ? TileType.STONE : TileType.DIRT;
                }
                TileType bg = isStoneDepth ? TileType.BACKGROUND_STONE : TileType.BACKGROUND_DIRT;
                chunk.backgroundBlocks[r][col] = bg;
                chunk.originalBackgroundBlocks[r][col] = bg;
            }

            surfaceRows[col] = bottomWaterRow + 1;
        }
    }

    private static void sealBankColumn(Chunk chunk, int col, int topRow, int bottomRow, int stoneStartRow) {
        for (int r = topRow; r <= bottomRow && r < EnginePanel.CHUNK_HEIGHT; r++) {
            if (!chunk.blocks[r][col].isSolid()) {
                chunk.blocks[r][col] = (r == topRow)
                        ? TileType.GRASS
                        : (r >= stoneStartRow ? TileType.STONE : TileType.DIRT);
            }
        }
    }

    public static void generateCaveWaterPools(Chunk chunk, int[] stoneStartRows, long seed) {
        int width = EnginePanel.CHUNK_WIDTH;
        int height = EnginePanel.CHUNK_HEIGHT;

        for (int row = height - 5; row >= 45; row--) {
            int col = 1;
            while (col < width - 1) {
                if (row < stoneStartRows[col] + 8 || chunk.blocks[row][col] != TileType.AIR) {
                    col++;
                    continue;
                }

                int spanStart = col;
                while (col < width - 1 && chunk.blocks[row][col] == TileType.AIR && row >= stoneStartRows[col] + 8) {
                    col++;
                }
                int spanEnd = col - 1;
                int spanWidth = spanEnd - spanStart + 1;

                if (spanWidth >= 2 && spanWidth <= 11
                        && chunk.blocks[row][spanStart - 1].isSolid()
                        && chunk.blocks[row][spanEnd + 1].isSolid()
                        && hasSealedFloor(chunk, row, spanStart, spanEnd)) {

                    boolean alreadyHasWaterBelow = chunk.getWaterLevel(spanStart, row + 1) > 0;
                    int basinHash = deterministicHash(seed + 9001L, (chunk.cx * 256 + row) * 31 + spanStart);

                    int depthBelow = countWaterDepthBelow(chunk, spanStart, row + 1);
                    if ((alreadyHasWaterBelow && depthBelow < 3) || (!alreadyHasWaterBelow && (basinHash & 0xFF) < 72)) {
                        for (int c = spanStart; c <= spanEnd; c++) {
                            chunk.blocks[row][c] = TileType.WATER;
                            chunk.setWaterData(c, row, FluidManager.MAX_WATER_LEVEL, true, false);
                        }
                    }
                }
            }
        }
    }

    private static boolean hasSealedFloor(Chunk chunk, int row, int spanStart, int spanEnd) {
        for (int c = spanStart; c <= spanEnd; c++) {
            TileType below = chunk.blocks[row + 1][c];
            if (!below.isSolid() && chunk.getWaterLevel(c, row + 1) < FluidManager.MAX_WATER_LEVEL) {
                return false;
            }
        }
        return true;
    }

    private static int countWaterDepthBelow(Chunk chunk, int col, int startRow) {
        int depth = 0;
        for (int r = startRow; r < EnginePanel.CHUNK_HEIGHT && depth < 5; r++) {
            if (chunk.getWaterLevel(col, r) > 0) {
                depth++;
            } else {
                break;
            }
        }
        return depth;
    }

    private static int deterministicHash(long seed, int salt) {
        long h = seed ^ (salt * 0x9E3779B97F4A7C15L);
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return (int) (h ^ (h >>> 31));
    }
}