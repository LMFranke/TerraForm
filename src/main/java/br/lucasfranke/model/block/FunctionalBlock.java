package br.lucasfranke.model.block;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;

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
}