package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.flora.FloraManager;
import br.lucasfranke.world.light.LightingEngine;

import java.util.HashMap;
import java.util.Map;

public class World {

    private final Map<Integer, Chunk> chunks = new HashMap<>();
    private final long seed;
    private final WorldGenerator generator;
    private final LightingEngine lighting;
    private final FloraManager flora;

    public World(long seed) {
        this.seed = seed;
        this.generator = new WorldGenerator(seed);
        this.lighting = new LightingEngine(this);
        this.flora = new FloraManager(this);
    }

    public long getSeed() {
        return seed;
    }

    public LightingEngine getLighting() {
        return lighting;
    }

    public FloraManager getFlora() {
        return flora;
    }

    public boolean isValidY(int tileY) {
        return tileY >= EnginePanel.MIN_WORLD_HEIGHT && tileY < EnginePanel.CHUNK_HEIGHT;
    }

    public int toChunkX(int tileX) {
        return Math.floorDiv(tileX, EnginePanel.CHUNK_WIDTH);
    }

    public boolean isChunkLoaded(int cx) {
        return chunks.containsKey(cx);
    }

    public Chunk getLoadedChunk(int cx) {
        return chunks.get(cx);
    }

    public Chunk getChunkByTileX(int tileX) {
        return getChunk(toChunkX(tileX));
    }

    public Chunk getChunk(int cx) {
        Chunk c = chunks.get(cx);
        if (c == null) {
            c = generator.generateChunk(cx);
            chunks.put(cx, c);
            flora.markScanDirty();
            lighting.calculateLightForChunkAndNeighbors(cx);
        } else if (c.lastSunLightLevel != lighting.getSunLightLevel()) {
            lighting.calculateLightForChunk(cx);
        }
        return c;
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
            flora.onForegroundBlockChanged(tileX, tileY, type);
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
            flora.markScanDirty();
        }
    }

    public FunctionalBlock getFunctionalBlock(int tileX, int tileY) {
        if (!isValidY(tileY)) return null;
        Chunk c = getChunkByTileX(tileX);
        return c != null ? c.getFunctionalBlock(tileX, tileY) : null;
    }

    public boolean isSolid(int tileX, int tileY) {
        return getBlock(tileX, tileY).isSolid();
    }

    public boolean isNaturalTreeTile(int tileX, int tileY) {
        if (!isValidY(tileY)) return false;
        Chunk c = getChunkByTileX(tileX);
        return c != null && c.isNaturalTreeTile(tileX, tileY);
    }

    public void setNaturalTreeTile(int tileX, int tileY, boolean isNaturalTree) {
        if (!isValidY(tileY)) return;
        Chunk c = getChunkByTileX(tileX);
        if (c != null) {
            c.setNaturalTreeTile(tileX, tileY, isNaturalTree);
            flora.markScanDirty();
        }
    }

    public void updateActiveFunctionalBlocks(int centerChunkX, int radius) {
        for (int cx = centerChunkX - radius; cx <= centerChunkX + radius; cx++) {
            Chunk c = chunks.get(cx);
            if (c != null) {
                c.updateFunctionalBlocks();
            }
        }
    }
}