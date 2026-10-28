package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.model.type.ToolCategory;
import br.lucasfranke.model.type.ToolTier;
import br.lucasfranke.sound.Sound;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class BlockInteractionController {

    private static final int MAX_INTERACTION_DISTANCE = 5;
    private static final int MAX_TREE_CANOPY_RADIUS = 3;

    private final World world;
    private final EntityManager entityManager;

    public final int[] hoveredTile = {-1, -1};

    private int miningTileX = -1;
    private int miningTileY = -1;
    private boolean miningBackground = false;
    private float miningProgress = 0f;
    private int miningTicks = 0;
    private int miningTargetHardness = 0;
    private int breakCooldown = 0;

    public BlockInteractionController(World world, EntityManager entityManager) {
        this.world = world;
        this.entityManager = entityManager;
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
        miningProgress = 0f;
        miningTicks = 0;
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
        TileType currentBg = world.getBackgroundBlock(tileX, tileY);
        TileType naturalBg = world.getNaturalBackgroundBlock(tileX, tileY);

        TileType targetTile = null;
        boolean targetIsBg = false;

        boolean canBreakBg = currentBg != TileType.AIR && currentBg != naturalBg && currentBg.isBreakable();
        boolean canBreakFg = currentFg != TileType.AIR && currentFg.isBreakable();

        if (backgroundMode) {
            if (canBreakBg) {
                targetTile = currentBg;
                targetIsBg = true;
            } else if (canBreakFg) {
                targetTile = currentFg;
            }
        } else {
            if (canBreakFg) {
                targetTile = currentFg;
            } else if (canBreakBg && world.isNaturalTreeTile(tileX, tileY)) {
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
            miningProgress = 0f;
            miningTicks = 0;
            miningTargetHardness = Math.max(1, targetTile.getHardness());
        }

        float speedMultiplier = calculateMiningSpeedMultiplier(player, targetTile);
        miningProgress += speedMultiplier;
        miningTicks++;

        if (miningTicks % 15 == 0 && miningProgress < miningTargetHardness) {
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

    private float calculateMiningSpeedMultiplier(Player player, TileType targetTile) {
        Item heldItem = player.hotBar.getCurrentItem();
        ToolCategory preferredTool = targetTile.getPreferredTool();
        ToolTier requiredTier = targetTile.getRequiredTier();

        ToolCategory heldCategory = (heldItem != null && heldItem.isTool())
                ? heldItem.getTileType().getToolCategory()
                : ToolCategory.NONE;
        ToolTier heldTier = (heldItem != null && heldItem.isTool())
                ? heldItem.getTileType().getToolTier()
                : ToolTier.NONE;

        if (preferredTool != ToolCategory.NONE && heldCategory == preferredTool) {
            if (heldTier.canHarvest(requiredTier)) {
                return heldTier.getEfficiencyMultiplier();
            }
            return 0.65f;
        }

        if (requiredTier.getHarvestLevel() >= ToolTier.STONE.getHarvestLevel()) {
            return 0.4f;
        }

        return 1.0f;
    }

    private void executeBlockBreak(Player player, int tileX, int tileY, TileType targetTile, boolean targetIsBg) {
        playSound(targetTile);

        TileType drop = targetTile.getDropType();
        if (drop != TileType.AIR) {
            player.addItem(drop, 1);
        }

        applyHeldToolDurabilityWear(player, targetTile);

        if (!targetIsBg) {
            FunctionalBlock fb = world.getFunctionalBlock(tileX, tileY);
            if (fb != null) {
                fb.onBreak(player);
            }
            world.setBlock(tileX, tileY, TileType.AIR);
            checkAndDropUnsupportedTorchesAround(player, tileX, tileY);
        } else {
            boolean wasNaturalTreeTrunk = (targetTile == TileType.BACKGROUND_WOOD)
                    && world.isNaturalTreeTile(tileX, tileY);

            world.setBackgroundBlock(tileX, tileY, world.getNaturalBackgroundBlock(tileX, tileY));
            checkAndDropUnsupportedTorchesAround(player, tileX, tileY);

            if (wasNaturalTreeTrunk) {
                collapseNaturalTreeAbove(player, tileX, tileY);
            }
        }
    }

    private void applyHeldToolDurabilityWear(Player player, TileType brokenTile) {
        if (brokenTile.getHardness() <= 1) {
            return;
        }
        Item heldItem = player.hotBar.getCurrentItem();
        if (heldItem != null && heldItem.isTool()) {
            boolean broke = heldItem.damage(1);
            if (broke) {
                player.hotBar.setItem(0, player.hotBar.getSelectedSlot(), null);
                player.hotBar.setCurrentItem(null);
            }
        }
    }

    private void collapseNaturalTreeAbove(Player player, int trunkX, int cutY) {
        int startY = cutY - 1;
        if (!world.isValidY(startY) || !world.isNaturalTreeTile(trunkX, startY)) {
            return;
        }

        Queue<Point> queue = new LinkedList<>();
        Set<Point> visited = new HashSet<>();

        Point root = new Point(trunkX, startY);
        queue.add(root);
        visited.add(root);

        int[] dx = {0, 0, -1, 1, -1, 1};
        int[] dy = {-1, 1, 0, 0, -1, -1};

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            int cx = current.x;
            int cy = current.y;

            TileType currentBg = world.getBackgroundBlock(cx, cy);
            boolean isTrunk = (currentBg == TileType.BACKGROUND_WOOD);
            boolean isLeaf = (currentBg == TileType.BACKGROUND_LEAVES);

            if (!isTrunk && !isLeaf) {
                continue;
            }

            world.setBackgroundBlock(cx, cy, world.getNaturalBackgroundBlock(cx, cy));
            TileType drop = currentBg.getDropType();
            if (drop != TileType.AIR) {
                player.addItem(drop, 1);
            }
            checkAndDropUnsupportedTorchesAround(player, cx, cy);

            for (int i = 0; i < dx.length; i++) {
                int nx = cx + dx[i];
                int ny = cy + dy[i];

                if (!world.isValidY(ny) || ny > cutY) {
                    continue;
                }
                if (Math.abs(nx - trunkX) > MAX_TREE_CANOPY_RADIUS) {
                    continue;
                }

                Point next = new Point(nx, ny);
                if (visited.contains(next) || !world.isNaturalTreeTile(nx, ny)) {
                    continue;
                }

                TileType nextBg = world.getBackgroundBlock(nx, ny);
                if (isTrunk) {
                    boolean nextIsSameTrunk = (nextBg == TileType.BACKGROUND_WOOD && nx == trunkX);
                    boolean nextIsLeaf = (nextBg == TileType.BACKGROUND_LEAVES);
                    if (nextIsSameTrunk || nextIsLeaf) {
                        visited.add(next);
                        queue.add(next);
                    }
                } else if (nextBg == TileType.BACKGROUND_LEAVES) {
                    visited.add(next);
                    queue.add(next);
                }
            }
        }
    }

    private void checkAndDropUnsupportedTorchesAround(Player player, int centerX, int centerY) {
        int[][] offsets = {{0, 0}, {0, -1}, {-1, 0}, {1, 0}};
        for (int[] offset : offsets) {
            int tx = centerX + offset[0];
            int ty = centerY + offset[1];
            if (world.isValidY(ty) && world.getBlock(tx, ty) == TileType.TORCH) {
                if (!hasTorchSupport(tx, ty)) {
                    world.setBlock(tx, ty, TileType.AIR);
                    player.addItem(TileType.TORCH, 1);
                }
            }
        }
    }

    private boolean hasTorchSupport(int tileX, int tileY) {
        if (world.getBackgroundBlock(tileX, tileY) != TileType.AIR) {
            return true;
        }
        if (world.isSolid(tileX, tileY + 1)) {
            return true;
        }
        return world.isSolid(tileX - 1, tileY) || world.isSolid(tileX + 1, tileY);
    }

    private boolean handlePlaceClick(Player player, int tileX, int tileY, boolean backgroundMode) {
        Item selectedItem = player.hotBar.getCurrentItem();
        if (selectedItem == null || selectedItem.getQuantity() <= 0) {
            return false;
        }

        TileType tileToPlace = selectedItem.getTileType();
        if (!tileToPlace.isPlaceable()) {
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
            return false;
        }

        if (world.getBlock(tileX, tileY) != TileType.AIR) {
            return false;
        }

        if (tileToPlace == TileType.TORCH) {
            if (!hasTorchSupport(tileX, tileY)) {
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
        if (player.getBounds().intersects(targetBounds)) {
            return true;
        }
        return entityManager != null && entityManager.intersectsAnyMob(targetBounds);
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
        return miningTargetHardness <= 0 ? 0f : Math.min(1.0f, miningProgress / miningTargetHardness);
    }
}