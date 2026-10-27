package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;

import java.awt.*;
import java.awt.event.MouseEvent;

public class BlockInteractionController {

    private static final int MAX_INTERACTION_DISTANCE = 5;

    private final World world;
    public final int[] hoveredTile = {-1, -1};

    private int miningTileX = -1;
    private int miningTileY = -1;
    private boolean miningBackground = false;
    private int miningProgress = 0;
    private int miningTargetHardness = 0;
    private int breakCooldown = 0;

    public BlockInteractionController(World world) {
        this.world = world;
    }

    public void update(Player player) {
        if (breakCooldown > 0) {
            breakCooldown--;
        }

        int tileSize = EnginePanel.tileSquare;
        int playerTileX = (int) (player.worldX / tileSize);
        int playerTileY = (int) (player.worldY / tileSize);

        int mouseWorldX = (int) (player.worldX - player.playerX + MouseManager.getX());
        int mouseWorldY = (int) (player.worldY - player.playerY + MouseManager.getY());

        int tileX = Math.floorDiv(mouseWorldX, tileSize);
        int tileY = Math.floorDiv(mouseWorldY, tileSize);

        int distance = Math.abs(tileX - playerTileX) + Math.abs(tileY - playerTileY);

        if (distance < MAX_INTERACTION_DISTANCE && world.isValidY(tileY)) {
            hoveredTile[0] = tileX;
            hoveredTile[1] = tileY;
        } else {
            hoveredTile[0] = -1;
            hoveredTile[1] = -1;
            resetMining();
        }
    }

    public void resetMining() {
        miningTileX = -1;
        miningTileY = -1;
        miningProgress = 0;
        miningTargetHardness = 0;
    }

    public void handleInteraction(Player player) {
        int tileX = hoveredTile[0];
        int tileY = hoveredTile[1];
        if (tileX == -1 || !world.isValidY(tileY)) {
            resetMining();
            return;
        }

        boolean backgroundMode = player.isBackgroundMode();
        int chunkX = world.toChunkX(tileX);

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
            if (breakCooldown == 0 && handleBreakProgress(player, tileX, tileY, backgroundMode)) {
                world.calculateLightForChunkAndNeighbors(chunkX);
            }
        } else if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
            FunctionalBlock fb = world.getFunctionalBlock(tileX, tileY);
            if (fb != null) {
                fb.onInteract(player);
                return;
            }
            if (handlePlaceClick(player, tileX, tileY, backgroundMode)) {
                world.calculateLightForChunkAndNeighbors(chunkX);
            }
        }
    }

    private boolean handleBreakProgress(Player player, int tileX, int tileY, boolean backgroundMode) {
        TileType currentFg = world.getBlock(tileX, tileY);
        TileType targetTile = null;
        boolean targetIsBg = false;

        if (currentFg != TileType.AIR && currentFg.isBreakable()) {
            targetTile = currentFg;
        } else if (backgroundMode) {
            TileType currentBg = world.getBackgroundBlock(tileX, tileY);
            TileType naturalBg = world.getNaturalBackgroundBlock(tileX, tileY);
            if (currentBg != TileType.AIR && currentBg != naturalBg) {
                targetTile = currentBg;
                targetIsBg = true;
            }
        }

        if (targetTile == null) {
            resetMining();
            return false;
        }

        if (tileX != miningTileX || tileY != miningTileY || targetIsBg != miningBackground) {
            miningTileX = tileX;
            miningTileY = tileY;
            miningBackground = targetIsBg;
            miningProgress = 0;
            miningTargetHardness = Math.max(1, targetTile.getHardness());
        }

        miningProgress++;

        if (miningProgress % 15 == 0 && miningProgress < miningTargetHardness) {
            playSound(targetTile);
        }

        if (miningProgress >= miningTargetHardness) {
            executeBlockBreak(player, tileX, tileY, targetTile, targetIsBg);
            resetMining();
            breakCooldown = 6;
            return true;
        }
        return false;
    }

    private void executeBlockBreak(Player player, int tileX, int tileY, TileType targetTile, boolean targetIsBg) {
        playSound(targetTile);

        TileType drop = targetTile.getDropType();
        if (drop != TileType.AIR) {
            player.addItem(drop, 1);
        }

        if (!targetIsBg) {
            FunctionalBlock fb = world.getFunctionalBlock(tileX, tileY);
            if (fb != null) {
                fb.onBreak(player);
            }

            world.setBlock(tileX, tileY, TileType.AIR);

            if (targetTile == TileType.WOOD) {
                collapseTreeAbove(player, tileX, tileY - 1);
            }

            if (tileY > 0 && world.getBlock(tileX, tileY - 1) == TileType.TORCH
                    && world.getBackgroundBlock(tileX, tileY - 1) == TileType.AIR) {
                world.setBlock(tileX, tileY - 1, TileType.AIR);
                player.addItem(TileType.TORCH, 1);
            }
        } else {
            world.setBackgroundBlock(tileX, tileY, world.getNaturalBackgroundBlock(tileX, tileY));
        }
    }

    private void collapseTreeAbove(Player player, int trunkX, int startY) {
        int currentY = startY;
        while (world.isValidY(currentY)) {
            TileType above = world.getBlock(trunkX, currentY);
            if (above != TileType.WOOD && above != TileType.LEAVES) {
                break;
            }

            for (int dx = -2; dx <= 2; dx++) {
                int lx = trunkX + dx;
                TileType neighbor = world.getBlock(lx, currentY);
                if (neighbor == TileType.WOOD || neighbor == TileType.LEAVES) {
                    world.setBlock(lx, currentY, TileType.AIR);
                    player.addItem(neighbor.getDropType(), 1);
                }
            }
            currentY--;
        }
    }

    private boolean handlePlaceClick(Player player, int tileX, int tileY, boolean backgroundMode) {
        Item selectedItem = player.hotBar.getCurrentItem();
        if (selectedItem == null || selectedItem.getQuantity() <= 0) {
            return false;
        }

        TileType tileToPlace = selectedItem.getTileType();
        if (!tileToPlace.isPlaceable() || world.getBlock(tileX, tileY) != TileType.AIR) {
            return false;
        }

        TileType currentBg = world.getBackgroundBlock(tileX, tileY);
        TileType naturalBg = world.getNaturalBackgroundBlock(tileX, tileY);

        if (backgroundMode && tileToPlace != TileType.TORCH) {
            TileType bgVariant = tileToPlace.getBackgroundVariant();
            if (bgVariant != TileType.AIR) {
                boolean isEmptySky = (currentBg == TileType.AIR);
                boolean isReplacingNaturalBg = (currentBg == naturalBg && currentBg != bgVariant);
                if (isEmptySky || isReplacingNaturalBg) {
                    world.setBackgroundBlock(tileX, tileY, bgVariant);
                    consumeSelectedItem(player, selectedItem);
                    return true;
                }
            }
        }

        if (tileToPlace == TileType.TORCH) {
            boolean hasBackgroundWall = (currentBg != TileType.AIR);
            boolean hasSolidGroundBelow = world.isSolid(tileX, tileY + 1);
            if (!hasBackgroundWall && !hasSolidGroundBelow) {
                return false;
            }
        }

        if (tileToPlace.isSolid() && intersectsPlayer(player, tileX, tileY)) {
            return false;
        }

        world.setBlock(tileX, tileY, tileToPlace);
        consumeSelectedItem(player, selectedItem);
        return true;
    }

    private boolean intersectsPlayer(Player player, int tileX, int tileY) {
        int size = EnginePanel.tileSquare;
        Rectangle targetBounds = new Rectangle(tileX * size, tileY * size, size, size);
        Rectangle playerBounds = new Rectangle((int) player.worldX, (int) player.worldY, player.width, player.height);
        return playerBounds.intersects(targetBounds);
    }

    private void consumeSelectedItem(Player player, Item selectedItem) {
        selectedItem.decreaseQuantity();
        if (selectedItem.getQuantity() <= 0) {
            player.hotBar.setItem(0, player.hotBar.getSelectedSlot(), null);
        }
    }

    private void playSound(TileType tile) {
        Sound sound = tile.getBreakSound();
        if (sound != null) {
            EnginePanel.soundManager.playSound(sound);
        }
    }

    public boolean isMiningTile(int tileX, int tileY) {
        return miningProgress > 0 && miningTargetHardness > 1 && tileX == miningTileX && tileY == miningTileY;
    }

    public float getMiningRatio() {
        return miningTargetHardness <= 0 ? 0f : Math.min(1.0f, (float) miningProgress / miningTargetHardness);
    }
}