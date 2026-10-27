package br.lucasfranke.model.block;

import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;

public class CraftingTableBlock extends FunctionalBlock {

    public CraftingTableBlock(int worldX, int worldY) {
        super(worldX, worldY, TileType.CRAFTING_TABLE, "Crafting Table", 3, 3);
    }

    private Rectangle getCraftButtonBounds(int panelX, int panelY, int panelWidth, int panelHeight) {
        return new Rectangle(panelX + panelWidth + 12, panelY + panelHeight / 2 - 20, 110, 40);
    }

    @Override
    public void drawExtraUI(Graphics2D g2, int panelX, int panelY, int panelWidth, int panelHeight) {
        Rectangle btn = getCraftButtonBounds(panelX, panelY, panelWidth, panelHeight);
        Item preview = findMatchingRecipeResult();

        g2.setColor(preview != null ? new Color(46, 139, 87, 230) : new Color(80, 80, 80, 200));
        g2.fillRoundRect(btn.x, btn.y, btn.width, btn.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(btn.x, btn.y, btn.width, btn.height, 10, 10);

        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        String label = preview != null ? "Create: " + preview.getTileType().name() : "No recipe";
        g2.drawString(label, btn.x + 10, btn.y + 25);
    }

    @Override
    public boolean handleExtraUIClick(int mouseX, int mouseY, Player player, int panelX, int panelY, int panelWidth, int panelHeight) {
        Rectangle btn = getCraftButtonBounds(panelX, panelY, panelWidth, panelHeight);
        if (btn.contains(mouseX, mouseY)) {
            Item result = findMatchingRecipeResult();
            if (result != null && player.addItem(result.getTileType(), result.getQuantity())) {
                consumeOneFromEachOccupiedSlot();
            }
            return true;
        }
        return false;
    }

    /**
     * - 1x WOOD                 -> 4x PLANKS
     * - 2x WOOD ou 2x PLANKS    -> 1x CRAFTING_TABLE
     * - 4x PLANKS ou 4x WOOD    -> 1x CHEST
     * - 4x STONE                -> 1x FURNACE
     * - 1x COAL + 1x WOOD/PLANKS -> 4x TORCH
     */
    private Item findMatchingRecipeResult() {
        int woodCount = countTilesInGrid(TileType.WOOD);
        int planksCount = countTilesInGrid(TileType.PLANKS);
        int stoneCount = countTilesInGrid(TileType.STONE);
        int coalCount = countTilesInGrid(TileType.COAL);
        int dirtCount = countTilesInGrid(TileType.DIRT);
        int grassCount = countTilesInGrid(TileType.GRASS);

        int totalOccupied = woodCount + planksCount + stoneCount + coalCount + dirtCount + grassCount;

        if (woodCount == 1 && totalOccupied == 1) {
            return new Item(TileType.PLANKS, 4);
        }
        if (coalCount == 1 && (woodCount == 1 || planksCount == 1) && totalOccupied == 2) {
            return new Item(TileType.TORCH, 4);
        }
        if ((planksCount == 4 || woodCount == 4 || dirtCount == 4) && totalOccupied == 4) {
            return new Item(TileType.CHEST, 1);
        }
        if ((planksCount == 2 || woodCount == 2 || grassCount == 2) && totalOccupied == 2) {
            return new Item(TileType.CRAFTING_TABLE, 1);
        }
        if (stoneCount == 4 && totalOccupied == 4) {
            return new Item(TileType.FURNACE, 1);
        }
        if (stoneCount == 1 && dirtCount == 1 && totalOccupied == 2) {
            return new Item(TileType.TORCH, 4);
        }
        return null;
    }

    private int countTilesInGrid(TileType type) {
        int count = 0;
        for (int r = 0; r < blockInventory.getRows(); r++) {
            for (int c = 0; c < blockInventory.getCols(); c++) {
                Item item = blockInventory.getItem(r, c);
                if (item != null && item.getTileType() == type && item.getQuantity() > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    private void consumeOneFromEachOccupiedSlot() {
        for (int r = 0; r < blockInventory.getRows(); r++) {
            for (int c = 0; c < blockInventory.getCols(); c++) {
                Item item = blockInventory.getItem(r, c);
                if (item != null) {
                    item.decreaseQuantity();
                    if (item.getQuantity() <= 0) {
                        blockInventory.setItem(r, c, null);
                    }
                }
            }
        }
    }
}