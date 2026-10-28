package br.lucasfranke.world.flora;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.Chunk;
import br.lucasfranke.world.World;

public final class TreeFeature {

    private TreeFeature() {
    }

    public static void placeInChunk(Chunk chunk, int centerCol, int baseRow, int trunkHeight) {
        int topRow = baseRow - trunkHeight + 1;
        if (topRow < 3) {
            return;
        }

        for (int dy = -2; dy <= 1; dy++) {
            int leafRow = topRow + dy;
            if (leafRow < 0 || leafRow >= EnginePanel.CHUNK_HEIGHT) {
                continue;
            }
            int radius = (dy == -2) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int leafCol = centerCol + dx;
                if (leafCol >= 0 && leafCol < EnginePanel.CHUNK_WIDTH) {
                    if (chunk.blocks[leafRow][leafCol] == TileType.AIR
                            && chunk.backgroundBlocks[leafRow][leafCol] == TileType.AIR) {
                        chunk.blocks[leafRow][leafCol] = TileType.OAK_LEAVES;
                        chunk.naturalTreeTiles[leafRow][leafCol] = true;
                    }
                }
            }
        }

        for (int r = baseRow; r >= topRow; r--) {
            chunk.backgroundBlocks[r][centerCol] = TileType.BACKGROUND_WOOD;
            chunk.naturalTreeTiles[r][centerCol] = true;
        }
    }

    public static boolean tryGrowInWorld(World world, int saplingX, int baseRow, int trunkHeight) {
        int topRow = baseRow - trunkHeight + 1;
        if (topRow < 3) {
            return false;
        }

        for (int r = baseRow; r >= topRow - 2; r--) {
            TileType fg = world.getBlock(saplingX, r);
            TileType bg = world.getBackgroundBlock(saplingX, r);
            boolean fgClear = (fg == TileType.AIR || fg == TileType.OAK_SAPLING || fg == TileType.OAK_LEAVES);
            boolean bgClear = (bg == TileType.AIR || bg == TileType.BACKGROUND_LEAVES);
            if (!fgClear || !bgClear) {
                return false;
            }
        }

        world.setBlock(saplingX, baseRow, TileType.AIR);

        for (int dy = -2; dy <= 1; dy++) {
            int leafRow = topRow + dy;
            if (!world.isValidY(leafRow)) {
                continue;
            }
            int radius = (dy == -2) ? 1 : 2;
            for (int dx = -radius; dx <= radius; dx++) {
                int leafCol = saplingX + dx;
                if (world.getBlock(leafCol, leafRow) == TileType.AIR
                        && world.getBackgroundBlock(leafCol, leafRow) == TileType.AIR) {
                    world.setBlock(leafCol, leafRow, TileType.OAK_LEAVES);
                    world.setNaturalTreeTile(leafCol, leafRow, true);
                }
            }
        }

        for (int r = baseRow; r >= topRow; r--) {
            world.setBackgroundBlock(saplingX, r, TileType.BACKGROUND_WOOD);
            world.setNaturalTreeTile(saplingX, r, true);
        }

        world.getLighting().calculateLightForChunkAndNeighbors(world.toChunkX(saplingX));
        return true;
    }
}