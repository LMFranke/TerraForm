package br.lucasfranke.model.block;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.ui.HotBarUI;
import br.lucasfranke.ui.InventoryUI;

import java.awt.*;
import java.awt.event.KeyEvent;

public class CraftingTableBlock extends FunctionalBlock {

    private static final int LEFT_PADDING = 20;
    private static final int RIGHT_PADDING = 24;
    private static final int TOP_PADDING = 22;
    private static final int BOTTOM_PADDING = 10;

    private static final int ARROW_GAP = 22;
    private static final int ARROW_WIDTH = 44;
    private static final int ARROW_HEIGHT = 24;

    private final boolean personalHandCrafting;

    public CraftingTableBlock(int worldX, int worldY) {
        this(worldX, worldY, false);
    }

    public CraftingTableBlock(int worldX, int worldY, boolean personalHandCrafting) {
        super(
                worldX,
                worldY,
                TileType.CRAFTING_TABLE,
                personalHandCrafting ? "Hand Crafting" : "Crafting Table",
                personalHandCrafting ? 2 : 3,
                personalHandCrafting ? 2 : 3
        );
        this.personalHandCrafting = personalHandCrafting;
    }

    @Override
    public boolean isWorldBlock() {
        return !personalHandCrafting;
    }

    @Override
    public Dimension getContentDimension() {
        int slotSize = blockInventory.getSlotSize();
        int gridW = blockInventory.getCols() * slotSize;
        int gridH = blockInventory.getRows() * slotSize;

        int totalWidth = LEFT_PADDING + gridW + (ARROW_GAP * 2) + ARROW_WIDTH + slotSize + RIGHT_PADDING;
        int totalHeight = TOP_PADDING + gridH + BOTTOM_PADDING;
        return new Dimension(totalWidth, totalHeight);
    }

    @Override
    public Rectangle getSlotBounds(int row, int col, int contentStartX, int contentStartY) {
        int slotSize = blockInventory.getSlotSize();
        return new Rectangle(
                contentStartX + LEFT_PADDING + col * slotSize,
                contentStartY + TOP_PADDING + row * slotSize,
                slotSize,
                slotSize
        );
    }

    private Rectangle getOutputSlotBounds(int contentStartX, int contentStartY) {
        int slotSize = blockInventory.getSlotSize();
        int gridW = blockInventory.getCols() * slotSize;
        int gridH = blockInventory.getRows() * slotSize;

        int outputX = contentStartX + LEFT_PADDING + gridW + (ARROW_GAP * 2) + ARROW_WIDTH;
        int outputY = contentStartY + TOP_PADDING + (gridH - slotSize) / 2;
        return new Rectangle(outputX, outputY, slotSize, slotSize);
    }

    @Override
    public void drawExtraUI(Graphics2D g2, int panelX, int panelY, int panelWidth, int panelHeight) {
        int contentStartX = panelX + 10;
        int contentStartY = panelY + 10;
        int slotSize = blockInventory.getSlotSize();
        int gridW = blockInventory.getCols() * slotSize;
        int gridH = blockInventory.getRows() * slotSize;

        int gridX = contentStartX + LEFT_PADDING;
        int gridY = contentStartY + TOP_PADDING;

        Rectangle outputRect = getOutputSlotBounds(contentStartX, contentStartY);
        Item preview = findMatchingRecipeResult();

        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        boolean outputHovered = outputRect.contains(mouseX, mouseY);


        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(new Color(220, 220, 220));
        g2.drawString("Input", gridX + 2, gridY - 6);

        g2.setColor(new Color(255, 215, 90));
        g2.drawString("Output", outputRect.x + 4, outputRect.y - 7);


        int arrowX = gridX + gridW + ARROW_GAP;
        int arrowY = gridY + (gridH - ARROW_HEIGHT) / 2;
        drawCraftingArrow(g2, arrowX, arrowY, ARROW_WIDTH, ARROW_HEIGHT, preview != null);


        InventoryUI.drawBeveledSlot(g2, outputRect.x, outputRect.y, outputRect.width, outputRect.height, outputHovered);

        Stroke oldStroke = g2.getStroke();
        g2.setColor(preview != null ? new Color(110, 230, 145, 230) : new Color(235, 185, 45, 210));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(outputRect.x - 4, outputRect.y - 4, outputRect.width + 8, outputRect.height + 8, 8, 8);
        g2.setStroke(oldStroke);


        if (preview != null && preview.getTileType().getSprite() != null) {
            int offSet = 10;
            int itemSize = outputRect.width - offSet;
            g2.drawImage(preview.getSprite(), outputRect.x + offSet / 2, outputRect.y + offSet / 2, itemSize, itemSize, null);
            HotBarUI.drawDurabilityBar(g2, preview, outputRect.x, outputRect.y, outputRect.width);
        }

        if (outputHovered) {
            InventoryUI.drawSlotHoverOutline(g2, outputRect.x, outputRect.y, outputRect.width, outputRect.height);
        }
    }

    private void drawCraftingArrow(Graphics2D g2, int x, int y, int width, int height, boolean active) {
        int headLength = 15;
        int shaftWidth = width - headLength;
        int shaftHeight = 10;
        int shaftY = y + (height - shaftHeight) / 2;

        Polygon arrow = new Polygon();
        arrow.addPoint(x, shaftY);
        arrow.addPoint(x + shaftWidth, shaftY);
        arrow.addPoint(x + shaftWidth, y);
        arrow.addPoint(x + width, y + height / 2);
        arrow.addPoint(x + shaftWidth, y + height);
        arrow.addPoint(x + shaftWidth, shaftY + shaftHeight);
        arrow.addPoint(x, shaftY + shaftHeight);

        if (active) {
            GradientPaint gp = new GradientPaint(
                    x, y, new Color(46, 160, 95),
                    x + width, y, new Color(110, 235, 145)
            );
            Paint oldPaint = g2.getPaint();
            g2.setPaint(gp);
            g2.fillPolygon(arrow);
            g2.setPaint(oldPaint);
            g2.setColor(new Color(210, 255, 220));
        } else {
            g2.setColor(new Color(30, 30, 35, 230));
            g2.fillPolygon(arrow);
            g2.setColor(new Color(120, 120, 130));
        }

        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(arrow);
    }

    @Override
    public boolean handleExtraUIClick(int mouseX, int mouseY, Player player, int panelX, int panelY, int panelWidth, int panelHeight) {
        int contentStartX = panelX + 10;
        int contentStartY = panelY + 10;
        Rectangle outputRect = getOutputSlotBounds(contentStartX, contentStartY);

        if (outputRect.contains(mouseX, mouseY)) {
            Item result = findMatchingRecipeResult();
            if (result == null) {
                return true;
            }

            boolean shiftPressed = KeyManager.isKeyPressed(KeyEvent.VK_SHIFT);
            if (shiftPressed) {
                if (player.addItem(result)) {
                    consumeOneFromEachOccupiedSlot();
                }
            } else {
                Item currentDragged = EnginePanel.uiManager.getDraggedItem();
                if (currentDragged == null) {
                    EnginePanel.uiManager.setDraggedItem(result);
                    consumeOneFromEachOccupiedSlot();
                } else if (!result.isTool()
                        && currentDragged.getTileType() == result.getTileType()
                        && currentDragged.getQuantity() + result.getQuantity() <= currentDragged.getMaxStackSize()) {
                    currentDragged.increaseQuantity(result.getQuantity());
                    consumeOneFromEachOccupiedSlot();
                }
            }
            return true;
        }
        return false;
    }

    private Item findMatchingRecipeResult() {
        int woodCount = countTilesInGrid(TileType.OAK_WOOD);
        int planksCount = countTilesInGrid(TileType.PLANKS);
        int stickCount = countTilesInGrid(TileType.STICK);
        int stoneCount = countTilesInGrid(TileType.STONE);
        int coalCount = countTilesInGrid(TileType.COAL);
        int ironCount = countTilesInGrid(TileType.IRON_INGOT);
        int goldCount = countTilesInGrid(TileType.GOLD_INGOT);
        int dirtCount = countTilesInGrid(TileType.DIRT);
        int grassCount = countTilesInGrid(TileType.GRASS);

        int totalOccupied = countAllOccupiedSlots();


        if (woodCount == 1 && totalOccupied == 1) {
            return new Item(TileType.PLANKS, 4);
        }


        if ((planksCount == 2 || woodCount == 2) && totalOccupied == 2) {
            return isVerticalPair()
                    ? new Item(TileType.STICK, 4)
                    : new Item(TileType.CRAFTING_TABLE, 1);
        }
        if (grassCount == 2 && totalOccupied == 2) {
            return new Item(TileType.CRAFTING_TABLE, 1);
        }


        if (coalCount == 1 && (stickCount == 1 || woodCount == 1 || planksCount == 1) && totalOccupied == 2) {
            return new Item(TileType.TORCH, 4);
        }
        if (stoneCount == 1 && dirtCount == 1 && totalOccupied == 2) {
            return new Item(TileType.TORCH, 4);
        }


        if ((planksCount == 4 || woodCount == 4 || dirtCount == 4) && totalOccupied == 4) {
            return new Item(TileType.CHEST, 1);
        }
        if (stoneCount == 4 && totalOccupied == 4) {
            return new Item(TileType.FURNACE, 1);
        }


        if (!personalHandCrafting) {
            Item woodTool = matchToolForMaterial(planksCount + woodCount, stickCount, totalOccupied,
                    TileType.WOODEN_PICKAXE, TileType.WOODEN_AXE, TileType.WOODEN_SHOVEL, TileType.WOODEN_SWORD);
            if (woodTool != null) return woodTool;

            Item stoneTool = matchToolForMaterial(stoneCount, stickCount, totalOccupied,
                    TileType.STONE_PICKAXE, TileType.STONE_AXE, TileType.STONE_SHOVEL, TileType.STONE_SWORD);
            if (stoneTool != null) return stoneTool;

            Item ironTool = matchToolForMaterial(ironCount, stickCount, totalOccupied,
                    TileType.IRON_PICKAXE, TileType.IRON_AXE, TileType.IRON_SHOVEL, TileType.IRON_SWORD);
            if (ironTool != null) return ironTool;

            Item goldTool = matchToolForMaterial(goldCount, stickCount, totalOccupied,
                    TileType.GOLD_PICKAXE, TileType.GOLD_AXE, TileType.GOLD_SHOVEL, TileType.GOLD_SWORD);
            if (goldTool != null) return goldTool;
        }

        return null;
    }

    private Item matchToolForMaterial(int materialCount, int stickCount, int totalOccupied,
                                      TileType pickaxe, TileType axe, TileType shovel, TileType sword) {
        if (materialCount == 3 && stickCount == 2 && totalOccupied == 5) {
            return isAxePattern() ? new Item(axe, 1) : new Item(pickaxe, 1);
        }
        if (materialCount == 1 && stickCount == 2 && totalOccupied == 3) {
            return new Item(shovel, 1);
        }
        if (materialCount == 2 && stickCount == 1 && totalOccupied == 3) {
            return new Item(sword, 1);
        }
        return null;
    }

    private boolean isVerticalPair() {
        int firstCol = -1;
        for (int r = 0; r < blockInventory.getRows(); r++) {
            for (int c = 0; c < blockInventory.getCols(); c++) {
                Item item = blockInventory.getItem(r, c);
                if (item != null && item.getQuantity() > 0) {
                    if (firstCol == -1) {
                        firstCol = c;
                    } else {
                        return firstCol == c;
                    }
                }
            }
        }
        return false;
    }

    private boolean isAxePattern() {
        if (blockInventory.getCols() < 3) {
            return false;
        }
        boolean topRowFull = blockInventory.getItem(0, 0) != null
                && blockInventory.getItem(0, 1) != null
                && blockInventory.getItem(0, 2) != null;
        return !topRowFull;
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

    private int countAllOccupiedSlots() {
        int count = 0;
        for (int r = 0; r < blockInventory.getRows(); r++) {
            for (int c = 0; c < blockInventory.getCols(); c++) {
                Item item = blockInventory.getItem(r, c);
                if (item != null && item.getQuantity() > 0) {
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