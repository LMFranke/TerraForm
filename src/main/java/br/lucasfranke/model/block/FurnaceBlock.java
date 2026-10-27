package br.lucasfranke.model.block;

import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.model.type.ToolTier;

import java.awt.*;

public class FurnaceBlock extends FunctionalBlock {

    private static final int CONTENT_WIDTH = 260;
    private static final int CONTENT_HEIGHT = 164;
    private static final int SMELT_TIME_TICKS = 180; // 3 sec in 60 UPS

    private int smeltProgress = 0;

    public FurnaceBlock(int worldX, int worldY) {
        super(worldX, worldY, TileType.FURNACE, "Furnace", 1, 3);
    }

    @Override
    public Dimension getContentDimension() {
        return new Dimension(CONTENT_WIDTH, CONTENT_HEIGHT);
    }

    @Override
    public Rectangle getSlotBounds(int row, int col, int contentStartX, int contentStartY) {
        int slotSize = blockInventory.getSlotSize();
        return switch (col) {
            case 0 -> new Rectangle(contentStartX + 26, contentStartY + 18, slotSize, slotSize);
            case 1 -> new Rectangle(contentStartX + 26, contentStartY + 102, slotSize, slotSize);
            case 2 -> new Rectangle(contentStartX + 182, contentStartY + 60, slotSize, slotSize);
            default -> super.getSlotBounds(row, col, contentStartX, contentStartY);
        };
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
        TileType fuelType = fuel.getTileType();
        if (fuelType.isTool() && fuelType.getToolTier() == ToolTier.WOOD) {
            return true;
        }
        return switch (fuelType) {
            case COAL, WOOD, PLANKS, STICK, TORCH, GRASS -> true;
            default -> false;
        };
    }

    private boolean canOutput(Item output, TileType resultType) {
        if (output == null) return true;
        return output.getTileType() == resultType && output.getQuantity() < Player.MAX_STACK_SIZE;
    }

    @Override
    public void drawExtraUI(Graphics2D g2, int panelX, int panelY, int panelWidth, int panelHeight) {
        int contentStartX = panelX + 10;
        int contentStartY = panelY + 10;
        int slotSize = blockInventory.getSlotSize();

        Rectangle inputRect = getSlotBounds(0, 0, contentStartX, contentStartY);
        Rectangle fuelRect = getSlotBounds(0, 1, contentStartX, contentStartY);
        Rectangle outputRect = getSlotBounds(0, 2, contentStartX, contentStartY);


        Stroke oldStroke = g2.getStroke();
        g2.setColor(new Color(235, 185, 45, 210));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(outputRect.x - 4, outputRect.y - 4, outputRect.width + 8, outputRect.height + 8, 8, 8);
        g2.setStroke(oldStroke);


        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("Input", inputRect.x + 9, inputRect.y - 5);
        g2.drawString("Fuel", fuelRect.x + 11, fuelRect.y + slotSize + 14);
        g2.setColor(new Color(255, 215, 90));
        g2.drawString("Output", outputRect.x + 4, outputRect.y - 7);


        int flameCenterX = inputRect.x + slotSize / 2;
        int flameCenterY = (inputRect.y + slotSize + fuelRect.y) / 2;
        drawFlameIndicator(g2, flameCenterX, flameCenterY, smeltProgress > 0);


        float ratio = (float) smeltProgress / SMELT_TIME_TICKS;
        int arrowX = inputRect.x + slotSize + 24;
        int arrowY = outputRect.y + slotSize / 2 - 12;
        int arrowW = outputRect.x - arrowX - 22;
        int arrowH = 24;
        drawSmeltingArrow(g2, arrowX, arrowY, arrowW, arrowH, ratio);


        Item input = blockInventory.getItem(0, 0);
        TileType resultType = getSmeltResult(input != null ? input.getTileType() : null);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        if (smeltProgress > 0 && resultType != null) {
            int pct = (int) (ratio * 100);
            g2.setColor(new Color(255, 195, 70));
            g2.drawString("Smelting: " + resultType.name() + " (" + pct + "%)", arrowX - 6, fuelRect.y + slotSize - 4);
        } else {
            g2.setColor(new Color(170, 170, 170));
            g2.drawString("Place ore + fuel", arrowX - 2, fuelRect.y + slotSize - 4);
        }
    }

    private void drawFlameIndicator(Graphics2D g2, int centerX, int centerY, boolean active) {
        int boxSize = 24;
        int x = centerX - boxSize / 2;
        int y = centerY - boxSize / 2;

        g2.setColor(new Color(30, 30, 35, 220));
        g2.fillRoundRect(x, y, boxSize, boxSize, 6, 6);
        g2.setColor(new Color(80, 80, 90));
        g2.drawRoundRect(x, y, boxSize, boxSize, 6, 6);

        if (!active) {
            g2.setColor(new Color(95, 95, 100));
            g2.fillRect(x + 5, y + 17, 14, 3);
            g2.fillRect(x + 8, y + 13, 8, 4);
            return;
        }

        int flicker = (int) ((System.currentTimeMillis() / 110) % 3);

        g2.setColor(new Color(255, 95, 20));
        g2.fillRect(x + 4, y + 10, 16, 10);
        g2.fillRect(x + 6, y + 5 + flicker, 4, 6);
        g2.fillRect(x + 11, y + 3 + (2 - flicker), 4, 8);
        g2.fillRect(x + 15, y + 7 + (flicker % 2), 3, 5);

        g2.setColor(new Color(255, 225, 60));
        g2.fillRect(x + 7, y + 12, 10, 7);
        g2.fillRect(x + 9, y + 8 + (flicker % 2), 6, 5);
    }

    private void drawSmeltingArrow(Graphics2D g2, int x, int y, int width, int height, float ratio) {
        int headLength = 16;
        int shaftWidth = width - headLength;
        int shaftHeight = 10;
        int shaftY = y + (height - shaftHeight) / 2;

        Polygon arrowPolygon = new Polygon();
        arrowPolygon.addPoint(x, shaftY);
        arrowPolygon.addPoint(x + shaftWidth, shaftY);
        arrowPolygon.addPoint(x + shaftWidth, y);
        arrowPolygon.addPoint(x + width, y + height / 2);
        arrowPolygon.addPoint(x + shaftWidth, y + height);
        arrowPolygon.addPoint(x + shaftWidth, shaftY + shaftHeight);
        arrowPolygon.addPoint(x, shaftY + shaftHeight);

        g2.setColor(new Color(30, 30, 35, 230));
        g2.fillPolygon(arrowPolygon);

        if (ratio > 0f) {
            Shape oldClip = g2.getClip();
            int fillW = Math.max(1, (int) (width * Math.min(1f, ratio)));
            g2.clipRect(x, y, fillW, height);

            GradientPaint gp = new GradientPaint(
                    x, y, new Color(255, 120, 20),
                    x + width, y, new Color(255, 220, 50)
            );
            Paint oldPaint = g2.getPaint();
            g2.setPaint(gp);
            g2.fillPolygon(arrowPolygon);
            g2.setPaint(oldPaint);
            g2.setClip(oldClip);
        }

        g2.setColor(new Color(120, 120, 130));
        g2.drawPolygon(arrowPolygon);
    }
}