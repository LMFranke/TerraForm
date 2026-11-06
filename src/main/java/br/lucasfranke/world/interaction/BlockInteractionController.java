package br.lucasfranke.world.interaction;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.model.type.tool.ToolCategory;
import br.lucasfranke.model.type.tool.ToolTier;
import br.lucasfranke.sound.Sound;
import br.lucasfranke.world.World;

import java.awt.*;
import java.awt.event.MouseEvent;

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
                world.getLighting().calculateLightForChunkAndNeighbors(chunkX);
            }
        } else if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
            FunctionalBlock fb = world.getFunctionalBlock(tileX, tileY);
            if (fb != null && !backgroundMode) {
                fb.onInteract(player);
                return;
            }
            if (handlePlaceClick(player, tileX, tileY, backgroundMode)) {
                world.getLighting().calculateLightForChunkAndNeighbors(chunkX);
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
            } else if (canBreakFg && !currentFg.isFunctional()) {
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
        applyHeldToolDurabilityWear(player, targetTile);

        if (!targetIsBg) {
            FunctionalBlock fb = world.getFunctionalBlock(tileX, tileY);
            if (fb != null) {
                fb.onBreak(player);
            }
            world.setBlock(tileX, tileY, TileType.AIR);
            spawnBrokenBlockDrops(player, tileX, tileY, targetTile);
            checkAndDropUnsupportedTorchesAround(player, tileX, tileY);
        } else {
            boolean wasNaturalTreeTrunk = (targetTile == TileType.BACKGROUND_WOOD)
                    && world.isNaturalTreeTile(tileX, tileY);
            world.setBackgroundBlock(tileX, tileY, world.getNaturalBackgroundBlock(tileX, tileY));
            spawnBrokenBlockDrops(player, tileX, tileY, targetTile);
            checkAndDropUnsupportedTorchesAround(player, tileX, tileY);
            if (wasNaturalTreeTrunk) {
                collapseNaturalTreeAbove(player, tileX, tileY);
            }
        }
    }

    private void spawnBrokenBlockDrops(Player player, int tileX, int tileY, TileType targetTile) {
        if (targetTile == TileType.OAK_LEAVES || targetTile == TileType.BACKGROUND_LEAVES) {
            Item held = player.hotBar.getCurrentItem();
            boolean holdingSword = held != null && held.isTool()
                    && held.getTileType().getToolCategory() == ToolCategory.SWORD;
            if (holdingSword) {
                entityManager.spawnTileDrop(tileX, tileY, new Item(TileType.OAK_LEAVES, 1));
            } else {
                world.getFlora().rollLeafDrops(tileX, tileY);
            }
        } else {
            TileType drop = targetTile.getDropType();
            if (drop != TileType.AIR) {
                entityManager.spawnTileDrop(tileX, tileY, new Item(drop, 1));
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
        for (int cy = cutY - 1; world.isValidY(cy); cy--) {
            if (!world.isNaturalTreeTile(trunkX, cy)
                    || world.getBackgroundBlock(trunkX, cy) != TileType.BACKGROUND_WOOD) {
                break;
            }
            TileType currentBg = world.getBackgroundBlock(trunkX, cy);
            world.setBackgroundBlock(trunkX, cy, world.getNaturalBackgroundBlock(trunkX, cy));
            TileType drop = currentBg.getDropType();
            if (drop != TileType.AIR) {
                entityManager.spawnTileDrop(trunkX, cy, new Item(drop, 1));
            }
            checkAndDropUnsupportedTorchesAround(player, trunkX, cy);
        }
    }

    private void checkAndDropUnsupportedTorchesAround(Player player, int centerX, int centerY) {
        int[][] offsets = {{0, 0}, {0, -1}, {-1, 0}, {1, 0}};
        for (int[] offset : offsets) {
            int tx = centerX + offset[0];
            int ty = centerY + offset[1];
            if (!world.isValidY(ty)) continue;

            TileType block = world.getBlock(tx, ty);
            if (block == TileType.TORCH && !hasTorchSupport(tx, ty)) {
                world.setBlock(tx, ty, TileType.AIR);
                entityManager.spawnTileDrop(tx, ty, new Item(TileType.TORCH, 1));
            } else if (block == TileType.OAK_SAPLING && !hasSaplingSupport(tx, ty)) {
                world.setBlock(tx, ty, TileType.AIR);
                entityManager.spawnTileDrop(tx, ty, new Item(TileType.OAK_SAPLING, 1));
            }
        }
    }

    private boolean hasSaplingSupport(int tileX, int tileY) {
        if (!world.isValidY(tileY + 1)) return false;
        TileType below = world.getBlock(tileX, tileY + 1);
        return below == TileType.GRASS || below == TileType.DIRT;
    }

    private boolean hasTorchSupport(int tileX, int tileY) {
        if (world.getBackgroundBlock(tileX, tileY) != TileType.AIR) {
            return true;
        }
        return world.isSolid(tileX, tileY + 1);
    }

    private boolean hasFunctionalBlockSupport(int tileX, int tileY) {
        if (world.getBackgroundBlock(tileX, tileY) != TileType.AIR) {
            return true;
        }

        return isSolidOrFunctional(tileX, tileY + 1)
                || isSolidOrFunctional(tileX, tileY - 1)
                || isSolidOrFunctional(tileX - 1, tileY)
                || isSolidOrFunctional(tileX + 1, tileY);
    }

    private boolean isSolidOrFunctional(int tx, int ty) {
        TileType block = world.getBlock(tx, ty);
        return block.isSolid() || block.isFunctional();
    }

    private boolean handlePlaceClick(Player player, int tileX, int tileY, boolean backgroundMode) {
        Item selectedItem = player.hotBar.getCurrentItem();
        if (selectedItem == null || selectedItem.getQuantity() <= 0) {
            return false;
        }
        TileType heldType = selectedItem.getTileType();

        if (heldType == TileType.BUCKET) {
            if (world.getFluid().collectWaterSource(tileX, tileY)) {
                replaceOrAddBucketItem(player, selectedItem, TileType.WATER_BUCKET);
                EnginePanel.soundManager.playSound(Sound.ITEM_PICKUP);
                return true;
            }
            return false;
        }

        if (heldType == TileType.WATER_BUCKET) {
            if (world.getFluid().placeWaterSource(tileX, tileY)) {
                replaceOrAddBucketItem(player, selectedItem, TileType.BUCKET);
                EnginePanel.soundManager.playSound(Sound.ITEM_PICKUP);
                return true;
            }
            return false;
        }

        if (!heldType.isPlaceable()) {
            return false;
        }

        TileType currentBg = world.getBackgroundBlock(tileX, tileY);
        TileType naturalBg = world.getNaturalBackgroundBlock(tileX, tileY);

        if (backgroundMode && heldType != TileType.TORCH) {
            TileType bgVariant = heldType.getBackgroundVariant();
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

        TileType existingFg = world.getBlock(tileX, tileY);
        if (existingFg != TileType.AIR && existingFg != TileType.WATER) {
            return false;
        }
        if (existingFg == TileType.WATER && (heldType == TileType.TORCH || heldType == TileType.OAK_SAPLING)) {
            return false;
        }

        if (heldType == TileType.TORCH) {
            if (!hasTorchSupport(tileX, tileY)) {
                return false;
            }
        } else if (heldType == TileType.OAK_SAPLING) {
            if (backgroundMode || !hasSaplingSupport(tileX, tileY)) {
                return false;
            }
        } else if (heldType.isFunctional()) {
            if (!hasFunctionalBlockSupport(tileX, tileY)) {
                return false;
            }
        }

        if (heldType.isSolid() && intersectsPlayer(player, tileX, tileY)) {
            return false;
        }

        world.setBlock(tileX, tileY, heldType);
        consumeSelectedItem(player, selectedItem);
        return true;
    }

    private void replaceOrAddBucketItem(Player player, Item selectedItem, TileType targetBucketType) {
        if (selectedItem.getQuantity() <= 1) {
            player.hotBar.setItem(0, player.hotBar.getSelectedSlot(), new Item(targetBucketType, 1));
        } else {
            selectedItem.decreaseQuantity();
            Item filledOrEmpty = new Item(targetBucketType, 1);
            if (!player.addItem(filledOrEmpty)) {
                player.dropItem(filledOrEmpty);
            }
        }
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