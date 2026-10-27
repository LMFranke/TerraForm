package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.util.Random;

public class WorldManager {

    public final World world;
    private final DayNightCycle dayNightCycle;
    private final BlockInteractionController interactionController;
    private final WorldRenderer renderer;

    public final int[] hoveredTileWorldPosition;

    public WorldManager() {
        this.world = new World(new Random().nextInt(100000));
        this.dayNightCycle = new DayNightCycle();
        this.interactionController = new BlockInteractionController(world);
        this.renderer = new WorldRenderer();
        this.hoveredTileWorldPosition = interactionController.hoveredTile;
    }

    public void spawnPlayerOnGround() {
        int playerTileX = (int) (EnginePanel.player.worldX / EnginePanel.tileSquare);
        int groundY = 0;

        while (groundY < EnginePanel.CHUNK_HEIGHT - 1) {
            if (world.isSolid(playerTileX, groundY + 1)) {
                break;
            }
            groundY++;
        }
        EnginePanel.player.worldY = groundY * EnginePanel.tileSquare;
    }

    public void update() {
        TileType.updateAnimations();

        int playerTileX = (int) (EnginePanel.player.worldX / EnginePanel.tileSquare);
        int playerChunkX = world.toChunkX(playerTileX);

        dayNightCycle.update(world, playerChunkX);
        world.updateActiveFunctionalBlocks(playerChunkX, 2);
        interactionController.update(EnginePanel.player);
    }

    public void draw(Graphics2D g2) {
        renderer.draw(g2, world, EnginePanel.player, interactionController);
    }

    public void handleBlockInteraction() {
        interactionController.handleInteraction(EnginePanel.player);
    }

    public void resetMining() {
        interactionController.resetMining();
    }

    public String getFormattedTime() {
        return dayNightCycle.getFormattedTime();
    }
}