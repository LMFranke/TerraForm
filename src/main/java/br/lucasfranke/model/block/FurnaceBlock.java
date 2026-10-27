package br.lucasfranke.model.block;

import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;

public class FurnaceBlock extends FunctionalBlock {

    private int smeltProgress = 0;
    private static final int SMELT_TIME_TICKS = 180; // 3 sec

    public FurnaceBlock(int worldX, int worldY) {
        super(worldX, worldY, TileType.FURNACE, "Furnace (Input | Fuel | Output)", 1, 3);
    }

    @Override
    public void update() {
        Item input = blockInventory.getItem(0, 0);
        Item fuel = blockInventory.getItem(0, 1);
        Item output = blockInventory.getItem(0, 2);

        TileType resultType = getSmeltResult(input != null ? input.getTileType() : null);

        if (resultType != null && isFuel(fuel) && canOutput(output, resultType)) {
            smeltProgress++;
            if (smeltProgress >= SMELT_TIME_TICKS) {
                smeltProgress = 0;


                input.decreaseQuantity();
                if (input.getQuantity() <= 0) blockInventory.setItem(0, 0, null);

                fuel.decreaseQuantity();
                if (fuel.getQuantity() <= 0) blockInventory.setItem(0, 1, null);


                if (output == null) {
                    blockInventory.setItem(0, 2, new Item(resultType, 1));
                } else {
                    output.increaseQuantity();
                }
            }
        } else {
            smeltProgress = 0;
        }
    }

    private TileType getSmeltResult(TileType inputType) {
        if (inputType == null) return null;
        return switch (inputType) {
            case IRON_ORE -> TileType.IRON_INGOT;
            case GOLD_ORE -> TileType.GOLD_INGOT;
            case WOOD -> TileType.COAL;
            case DIRT, GRASS -> TileType.STONE;
            default -> null;
        };
    }

    private boolean isFuel(Item fuel) {
        if (fuel == null || fuel.getQuantity() <= 0) return false;
        return switch (fuel.getTileType()) {
            case COAL, WOOD, PLANKS, TORCH, GRASS -> true;
            default -> false;
        };
    }

    private boolean canOutput(Item output, TileType resultType) {
        if (output == null) return true;
        return output.getTileType() == resultType && output.getQuantity() < Player.MAX_STACK_SIZE;
    }

    @Override
    public void drawExtraUI(Graphics2D g2, int panelX, int panelY, int panelWidth, int panelHeight) {
        float ratio = (float) smeltProgress / SMELT_TIME_TICKS;
        int barWidth = panelWidth - 20;
        int barHeight = 8;
        int barX = panelX + 10;
        int barY = panelY + panelHeight - 14;

        g2.setColor(new Color(30, 30, 30, 220));
        g2.fillRect(barX, barY, barWidth, barHeight);
        g2.setColor(new Color(255, 140, 0));
        g2.fillRect(barX + 1, barY + 1, (int) ((barWidth - 2) * ratio), barHeight - 2);
    }
}