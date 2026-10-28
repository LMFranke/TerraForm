package br.lucasfranke.model.block;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.ItemContainer;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class FunctionalBlock {

    protected final int worldX;
    protected final int worldY;
    protected final TileType tileType;
    protected final String title;
    protected final Inventory blockInventory;

    public FunctionalBlock(int worldX, int worldY, TileType tileType, String title, int rows, int cols) {
        this.worldX = worldX;
        this.worldY = worldY;
        this.tileType = tileType;
        this.title = title;
        this.blockInventory = new Inventory(rows, cols, true);
    }

    public void onInteract(Player player) {
        EnginePanel.uiManager.openFunctionalBlock(this);
    }

    public void update() {
    }

    public boolean isOutputSlot(int row, int col) {
        return false;
    }

    public boolean canPlaceItemInSlot(int row, int col, Item item) {
        return !isOutputSlot(row, col);
    }

    public boolean tryShiftClickInsert(Item item) {
        return insertIntoContainer(blockInventory, item, 0, blockInventory.getRows() - 1, 0, blockInventory.getCols() - 1);
    }

    protected boolean insertIntoContainer(ItemContainer container, Item sourceItem, int minRow, int maxRow, int minCol, int maxCol) {
        if (sourceItem == null || sourceItem.getQuantity() <= 0) {
            return false;
        }
        int initialQty = sourceItem.getQuantity();

        if (!sourceItem.isTool()) {
            for (int r = minRow; r <= maxRow; r++) {
                for (int c = minCol; c <= maxCol; c++) {
                    if (!canPlaceItemInSlot(r, c, sourceItem)) continue;
                    Item slotItem = container.getItem(r, c);
                    if (slotItem != null && slotItem.getTileType() == sourceItem.getTileType()
                            && slotItem.getQuantity() < slotItem.getMaxStackSize()) {
                        int space = slotItem.getMaxStackSize() - slotItem.getQuantity();
                        int toMove = Math.min(space, sourceItem.getQuantity());
                        slotItem.increaseQuantity(toMove);
                        sourceItem.setQuantity(sourceItem.getQuantity() - toMove);
                        if (sourceItem.getQuantity() <= 0) {
                            return true;
                        }
                    }
                }
            }
        }

        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (!canPlaceItemInSlot(r, c, sourceItem)) continue;
                if (container.getItem(r, c) == null) {
                    int toPlace = Math.min(sourceItem.getMaxStackSize(), sourceItem.getQuantity());
                    container.setItem(r, c, sourceItem.copyWithQuantity(toPlace));
                    sourceItem.setQuantity(sourceItem.getQuantity() - toPlace);
                    if (sourceItem.getQuantity() <= 0) {
                        return true;
                    }
                }
            }
        }

        return sourceItem.getQuantity() < initialQty;
    }

    public void returnItemsToPlayer(Player player) {
        for (int r = 0; r < blockInventory.getRows(); r++) {
            for (int c = 0; c < blockInventory.getCols(); c++) {
                Item item = blockInventory.getItem(r, c);
                if (item != null && item.getQuantity() > 0) {
                    player.addItem(item);
                    blockInventory.setItem(r, c, null);
                }
            }
        }
    }

    public void onBreak(Player player) {
        returnItemsToPlayer(player);
        if (EnginePanel.uiManager.getCurrentlyOpenFunctionalBlock() == this) {
            EnginePanel.uiManager.closeInventory();
        }
    }

    public boolean isWorldBlock() {
        return true;
    }

    public Dimension getContentDimension() {
        int slotSize = blockInventory.getSlotSize();
        return new Dimension(blockInventory.getCols() * slotSize, blockInventory.getRows() * slotSize);
    }

    public Rectangle getSlotBounds(int row, int col, int contentStartX, int contentStartY) {
        int slotSize = blockInventory.getSlotSize();
        return new Rectangle(
                contentStartX + col * slotSize,
                contentStartY + row * slotSize,
                slotSize,
                slotSize
        );
    }

    public void drawExtraUI(Graphics2D g2, int panelX, int panelY, int panelWidth, int panelHeight) {
    }

    public boolean handleExtraUIClick(int mouseX, int mouseY, Player player, int panelX, int panelY, int panelWidth, int panelHeight) {
        return false;
    }

    public int getWorldX() {
        return worldX;
    }

    public int getWorldY() {
        return worldY;
    }

    public TileType getTileType() {
        return tileType;
    }

    public String getTitle() {
        return title;
    }

    public Inventory getBlockInventory() {
        return blockInventory;
    }

    public static FunctionalBlock create(TileType type, int worldX, int worldY) {
        return switch (type) {
            case CHEST -> new ChestBlock(worldX, worldY);
            case FURNACE -> new FurnaceBlock(worldX, worldY);
            case CRAFTING_TABLE -> new CraftingTableBlock(worldX, worldY);
            default -> null;
        };
    }

    public BufferedImage getRenderSprite() {
        return tileType.getSprite();
    }

}