package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;

public class InventoryUI {

    private static final int HEADER_HEIGHT = 38;
    private static final int BOTTOM_PADDING = 14;
    private static final int SIDE_PADDING = 12;
    private static final int PANEL_GAP = 14;

    private static final Color BG_TINT_COLOR = new Color(8, 10, 15, 145);
    private static final Color PANEL_BG_COLOR = new Color(36, 38, 45, 240);
    private static final Color PANEL_HEADER_COLOR = new Color(26, 28, 34, 245);
    private static final Color PANEL_BORDER_OUTER = new Color(18, 20, 24, 250);
    private static final Color PANEL_BORDER_INNER = new Color(90, 95, 110, 190);

    private static final Color SLOT_BG_COLOR = new Color(58, 62, 72, 230);
    private static final Color SLOT_HOVER_BG_COLOR = new Color(82, 88, 102, 240);
    private static final Color SLOT_SHADOW_DARK = new Color(24, 26, 32, 230);
    private static final Color SLOT_HIGHLIGHT_LIGHT = new Color(105, 110, 125, 180);
    private static final Color HOVER_BORDER_COLOR = new Color(255, 235, 130, 235);

    public void draw(Graphics2D g2, Inventory playerInventory, FunctionalBlock functionalBlock, int width, int height) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(BG_TINT_COLOR);
        g2.fillRect(0, 0, width, height);

        if (functionalBlock != null && playerInventory != null) {
            Point blockStart = getFunctionalBlockStart(functionalBlock, playerInventory);
            Point playerStart = getPlayerInventoryStart(playerInventory, functionalBlock);

            drawPanelConnectorBridge(g2, functionalBlock, blockStart, playerInventory, playerStart);
            drawFunctionalBlockPanel(g2, functionalBlock, blockStart.x, blockStart.y);
            drawContainerPanel(g2, playerInventory, playerStart.x, playerStart.y, "Backpack Inventory", TileType.CHEST.getSprite());
        } else if (playerInventory != null) {
            Point playerStart = getPlayerInventoryStart(playerInventory, null);
            drawContainerPanel(g2, playerInventory, playerStart.x, playerStart.y, "Backpack Inventory", TileType.CHEST.getSprite());
        }
    }

    private void drawPanelConnectorBridge(Graphics2D g2, FunctionalBlock fb, Point blockStart, Inventory playerInv, Point playerStart) {
        Dimension topDim = fb.getContentDimension();
        int topBottomY = blockStart.y + topDim.height + BOTTOM_PADDING;
        int botTopY = playerStart.y - HEADER_HEIGHT;

        int minWidth = Math.min(topDim.width, playerInv.getCols() * playerInv.getSlotSize());
        int centerX = EnginePanel.WIDTH / 2;
        int pillarOffset = Math.max(40, minWidth / 2 - 36);

        g2.setColor(new Color(28, 30, 36, 220));
        g2.fillRect(centerX - pillarOffset - 6, topBottomY, 12, botTopY - topBottomY);
        g2.fillRect(centerX + pillarOffset - 6, topBottomY, 12, botTopY - topBottomY);

        g2.setColor(new Color(85, 90, 105, 180));
        g2.drawLine(centerX - pillarOffset, topBottomY, centerX - pillarOffset, botTopY);
        g2.drawLine(centerX + pillarOffset, topBottomY, centerX + pillarOffset, botTopY);
    }

    private void drawFunctionalBlockPanel(Graphics2D g2, FunctionalBlock functionalBlock, int startX, int startY) {
        Inventory inventory = functionalBlock.getBlockInventory();
        Dimension contentDim = functionalBlock.getContentDimension();

        drawStyledWindowFrame(
                g2,
                startX,
                startY,
                contentDim.width,
                contentDim.height,
                functionalBlock.getTitle(),
                functionalBlock.getTileType().getSprite()
        );

        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        Rectangle hoveredRect = null;

        for (int row = 0; row < inventory.getRows(); row++) {
            for (int col = 0; col < inventory.getCols(); col++) {
                Rectangle slotRect = functionalBlock.getSlotBounds(row, col, startX, startY);
                boolean isHovered = slotRect.contains(mouseX, mouseY);

                drawBeveledSlot(g2, slotRect.x, slotRect.y, slotRect.width, slotRect.height, isHovered);

                Item item = inventory.getItem(row, col);
                if (item != null && item.getTileType().getSprite() != null) {
                    if (item.getQuantity() <= 0) {
                        inventory.setItem(row, col, null);
                    } else {
                        int offSet = 10;
                        int itemSize = slotRect.width - offSet;
                        g2.drawImage(item.getSprite(), slotRect.x + offSet / 2, slotRect.y + offSet / 2, itemSize, itemSize, null);
                        HotBarUI.drawDurabilityBar(g2, item, slotRect.x, slotRect.y, slotRect.width);
                    }
                }

                if (isHovered) {
                    hoveredRect = slotRect;
                }
            }
        }

        int panelW = contentDim.width + 20;
        int panelH = contentDim.height + 28;
        functionalBlock.drawExtraUI(g2, startX - 10, startY - 10, panelW, panelH);

        if (hoveredRect != null) {
            drawSlotHoverOutline(g2, hoveredRect.x, hoveredRect.y, hoveredRect.width, hoveredRect.height);
        }
    }

    private void drawContainerPanel(Graphics2D g2, Inventory inventory, int startX, int startY, String title, BufferedImage icon) {
        int slotSize = inventory.getSlotSize();
        int gridW = inventory.getCols() * slotSize;
        int gridH = inventory.getRows() * slotSize;

        drawStyledWindowFrame(g2, startX, startY, gridW, gridH, title, icon);

        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        Rectangle hoveredRect = null;

        for (int row = 0; row < inventory.getRows(); row++) {
            for (int col = 0; col < inventory.getCols(); col++) {
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;
                boolean isHovered = mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize;

                drawBeveledSlot(g2, x, y, slotSize, slotSize, isHovered);

                Item item = inventory.getItem(row, col);
                if (item != null && item.getTileType().getSprite() != null) {
                    if (item.getQuantity() <= 0) {
                        inventory.setItem(row, col, null);
                    } else {
                        int offSet = 10;
                        int itemSize = slotSize - offSet;
                        g2.drawImage(item.getSprite(), x + offSet / 2, y + offSet / 2, itemSize, itemSize, null);
                        HotBarUI.drawDurabilityBar(g2, item, x, y, slotSize);
                    }
                }

                if (isHovered) {
                    hoveredRect = new Rectangle(x, y, slotSize, slotSize);
                }
            }
        }

        if (hoveredRect != null) {
            drawSlotHoverOutline(g2, hoveredRect.x, hoveredRect.y, hoveredRect.width, hoveredRect.height);
        }
    }

    private void drawStyledWindowFrame(Graphics2D g2, int startX, int startY, int contentW, int contentH, String title, BufferedImage icon) {
        int frameX = startX - SIDE_PADDING;
        int frameY = startY - HEADER_HEIGHT;
        int frameW = contentW + SIDE_PADDING * 2;
        int frameH = contentH + HEADER_HEIGHT + BOTTOM_PADDING;


        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillRoundRect(frameX + 4, frameY + 5, frameW, frameH, 16, 16);


        g2.setColor(PANEL_BG_COLOR);
        g2.fillRoundRect(frameX, frameY, frameW, frameH, 14, 14);


        g2.setColor(PANEL_HEADER_COLOR);
        g2.fillRoundRect(frameX, frameY, frameW, HEADER_HEIGHT - 4, 14, 14);
        g2.fillRect(frameX, frameY + 12, frameW, HEADER_HEIGHT - 16);


        g2.setColor(new Color(75, 80, 95, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(frameX + 2, startY - 6, frameX + frameW - 2, startY - 6);


        g2.setColor(PANEL_BORDER_INNER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(frameX + 1, frameY + 1, frameW - 2, frameH - 2, 14, 14);

        g2.setColor(PANEL_BORDER_OUTER);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(frameX, frameY, frameW, frameH, 14, 14);


        int titleX = startX + 2;
        if (icon != null) {
            g2.drawImage(icon, startX, frameY + 8, 20, 20, null);
            titleX += 26;
        }

        g2.setFont(new Font("SansSerif", Font.BOLD, 15));
        g2.setColor(new Color(0, 0, 0, 180));
        g2.drawString(title, titleX + 1, frameY + 24);
        g2.setColor(new Color(240, 242, 248));
        g2.drawString(title, titleX, frameY + 23);
    }

    public static void drawBeveledSlot(Graphics2D g2, int x, int y, int w, int h, boolean hovered) {
        int margin = 2;
        int sx = x + margin;
        int sy = y + margin;
        int sw = w - margin * 2;
        int sh = h - margin * 2;

        // Fundo do slot
        g2.setColor(hovered ? SLOT_HOVER_BG_COLOR : SLOT_BG_COLOR);
        g2.fillRoundRect(sx, sy, sw, sh, 6, 6);

        // Sombra superior/esquerda (efeito de slot rebaixado)
        g2.setColor(SLOT_SHADOW_DARK);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(sx + 1, sy + 1, sx + sw - 2, sy + 1);
        g2.drawLine(sx + 1, sy + 1, sx + 1, sy + sh - 2);

        // Brilho inferior/direito
        g2.setColor(SLOT_HIGHLIGHT_LIGHT);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(sx + 2, sy + sh - 1, sx + sw - 1, sy + sh - 1);
        g2.drawLine(sx + sw - 1, sy + 2, sx + sw - 1, sy + sh - 1);

        // Contorno fino externo
        g2.setColor(new Color(20, 22, 26, 220));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(sx, sy, sw, sh, 6, 6);
    }

    public static void drawSlotHoverOutline(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(HOVER_BORDER_COLOR);
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 7, 7);
    }

    public static Point getFunctionalBlockStart(FunctionalBlock functionalBlock) {
        return getFunctionalBlockStart(functionalBlock, EnginePanel.player != null ? EnginePanel.player.inventory : null);
    }

    public static Point getFunctionalBlockStart(FunctionalBlock functionalBlock, Inventory playerInv) {
        Dimension topDim = functionalBlock.getContentDimension();
        int topFrameH = topDim.height + HEADER_HEIGHT + BOTTOM_PADDING;

        int botContentH = (playerInv != null) ? playerInv.getRows() * playerInv.getSlotSize() : 5 * EnginePanel.tileSquare;
        int botFrameH = botContentH + HEADER_HEIGHT + BOTTOM_PADDING;

        int totalGroupH = topFrameH + PANEL_GAP + botFrameH;
        int usableScreenH = (int) (EnginePanel.HEIGHT * 0.90);
        int groupTopY = Math.max(20, (usableScreenH - totalGroupH) / 2);

        int startX = (EnginePanel.WIDTH - topDim.width) / 2;
        int startY = groupTopY + HEADER_HEIGHT;
        return new Point(startX, startY);
    }

    public static Point getPlayerInventoryStart(Inventory inv, FunctionalBlock openBlock) {
        int startX = (EnginePanel.WIDTH - inv.getCols() * inv.getSlotSize()) / 2;
        if (openBlock != null) {
            Point blockStart = getFunctionalBlockStart(openBlock, inv);
            Dimension topDim = openBlock.getContentDimension();
            int topPanelBottom = blockStart.y + topDim.height + BOTTOM_PADDING;
            int startY = topPanelBottom + PANEL_GAP + HEADER_HEIGHT;
            return new Point(startX, startY);
        }
        int startY = (EnginePanel.HEIGHT - inv.getRows() * inv.getSlotSize()) / 2;
        return new Point(startX, startY);
    }

    public static Point getPlayerInventoryStart(Inventory inv, boolean hasFunctionalBlockOpen) {
        FunctionalBlock openBlock = (hasFunctionalBlockOpen && EnginePanel.uiManager != null)
                ? EnginePanel.uiManager.getCurrentlyOpenFunctionalBlock()
                : null;
        return getPlayerInventoryStart(inv, openBlock);
    }
}