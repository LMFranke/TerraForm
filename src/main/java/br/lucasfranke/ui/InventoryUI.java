package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

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

    private static final Stroke STROKE_1F = new BasicStroke(1f);
    private static final Stroke STROKE_1_5F = new BasicStroke(1.5f);
    private static final Stroke STROKE_2F = new BasicStroke(2f);
    private static final Stroke STROKE_2_5F = new BasicStroke(2.5f);

    private static BufferedImage cachedSlotNormal;
    private static BufferedImage cachedSlotHovered;
    private static BufferedImage cachedHoverOutline;

    private final Map<String, BufferedImage> windowFrameCache = new HashMap<>();

    public void draw(Graphics2D g2, Inventory playerInventory, FunctionalBlock functionalBlock, int width, int height) {
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

        drawCachedWindowFrame(
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
                        BufferedImage itemSprite = item.getSprite();
                        int expectedSize = slotRect.width - offSet;
                        if (itemSprite.getWidth() == expectedSize && itemSprite.getHeight() == expectedSize) {
                            g2.drawImage(itemSprite, slotRect.x + offSet / 2, slotRect.y + offSet / 2, null);
                        } else {
                            g2.drawImage(itemSprite, slotRect.x + offSet / 2, slotRect.y + offSet / 2, expectedSize, expectedSize, null);
                        }
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

        drawCachedWindowFrame(g2, startX, startY, gridW, gridH, title, icon);

        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        int hoveredX = -1;
        int hoveredY = -1;

        int offSet = 10;
        int expectedSize = slotSize - offSet;

        for (int row = 0; row < inventory.getRows(); row++) {
            int y = startY + row * slotSize;
            for (int col = 0; col < inventory.getCols(); col++) {
                int x = startX + col * slotSize;
                boolean isHovered = mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize;

                drawBeveledSlot(g2, x, y, slotSize, slotSize, isHovered);

                Item item = inventory.getItem(row, col);
                if (item != null && item.getTileType().getSprite() != null) {
                    if (item.getQuantity() <= 0) {
                        inventory.setItem(row, col, null);
                    } else {
                        BufferedImage itemSprite = item.getSprite();
                        if (itemSprite.getWidth() == expectedSize && itemSprite.getHeight() == expectedSize) {
                            g2.drawImage(itemSprite, x + offSet / 2, y + offSet / 2, null);
                        } else {
                            g2.drawImage(itemSprite, x + offSet / 2, y + offSet / 2, expectedSize, expectedSize, null);
                        }
                        HotBarUI.drawDurabilityBar(g2, item, x, y, slotSize);
                    }
                }

                if (isHovered) {
                    hoveredX = x;
                    hoveredY = y;
                }
            }
        }

        if (hoveredX != -1) {
            drawSlotHoverOutline(g2, hoveredX, hoveredY, slotSize, slotSize);
        }
    }

    private void drawCachedWindowFrame(Graphics2D g2, int startX, int startY, int contentW, int contentH, String title, BufferedImage icon) {
        String key = title + "_" + contentW + "x" + contentH;
        BufferedImage cached = windowFrameCache.get(key);

        int frameX = startX - SIDE_PADDING;
        int frameY = startY - HEADER_HEIGHT;

        if (cached == null) {
            int frameW = contentW + SIDE_PADDING * 2;
            int frameH = contentH + HEADER_HEIGHT + BOTTOM_PADDING;
            // +6 pixels de margem para acomodar a sombra projetada
            cached = createCompatibleImage(frameW + 6, frameH + 6);
            Graphics2D bg = cached.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            bg.setColor(new Color(0, 0, 0, 110));
            bg.fillRoundRect(4, 5, frameW, frameH, 16, 16);

            bg.setColor(PANEL_BG_COLOR);
            bg.fillRoundRect(0, 0, frameW, frameH, 14, 14);

            bg.setColor(PANEL_HEADER_COLOR);
            bg.fillRoundRect(0, 0, frameW, HEADER_HEIGHT - 4, 14, 14);
            bg.fillRect(0, 12, frameW, HEADER_HEIGHT - 16);

            bg.setColor(new Color(75, 80, 95, 200));
            bg.setStroke(STROKE_1_5F);
            bg.drawLine(2, HEADER_HEIGHT - 6, frameW - 2, HEADER_HEIGHT - 6);

            bg.setColor(PANEL_BORDER_INNER);
            bg.setStroke(STROKE_2F);
            bg.drawRoundRect(1, 1, frameW - 2, frameH - 2, 14, 14);

            bg.setColor(PANEL_BORDER_OUTER);
            bg.setStroke(STROKE_1_5F);
            bg.drawRoundRect(0, 0, frameW, frameH, 14, 14);

            int titleX = SIDE_PADDING + 2;
            if (icon != null) {
                bg.drawImage(icon, SIDE_PADDING, 8, 20, 20, null);
                titleX += 26;
            }

            bg.setFont(new Font("SansSerif", Font.BOLD, 15));
            bg.setColor(new Color(0, 0, 0, 180));
            bg.drawString(title, titleX + 1, 24);
            bg.setColor(new Color(240, 242, 248));
            bg.drawString(title, titleX, 23);

            bg.dispose();
            windowFrameCache.put(key, cached);
        }

        g2.drawImage(cached, frameX, frameY, null);
    }

    public static void drawBeveledSlot(Graphics2D g2, int x, int y, int w, int h, boolean hovered) {
        if (w == EnginePanel.tileSquare && h == EnginePanel.tileSquare) {
            if (cachedSlotNormal == null) {
                cachedSlotNormal = renderSlotToImage(w, h, false);
                cachedSlotHovered = renderSlotToImage(w, h, true);
            }
            g2.drawImage(hovered ? cachedSlotHovered : cachedSlotNormal, x, y, null);
            return;
        }
        drawBeveledSlotDirect(g2, x, y, w, h, hovered);
    }

    private static BufferedImage renderSlotToImage(int w, int h, boolean hovered) {
        BufferedImage img = createCompatibleImage(w, h);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawBeveledSlotDirect(g2, 0, 0, w, h, hovered);
        g2.dispose();
        return img;
    }

    private static void drawBeveledSlotDirect(Graphics2D g2, int x, int y, int w, int h, boolean hovered) {
        int margin = 2;
        int sx = x + margin;
        int sy = y + margin;
        int sw = w - margin * 2;
        int sh = h - margin * 2;

        g2.setColor(hovered ? SLOT_HOVER_BG_COLOR : SLOT_BG_COLOR);
        g2.fillRoundRect(sx, sy, sw, sh, 6, 6);

        g2.setColor(SLOT_SHADOW_DARK);
        g2.setStroke(STROKE_2F);
        g2.drawLine(sx + 1, sy + 1, sx + sw - 2, sy + 1);
        g2.drawLine(sx + 1, sy + 1, sx + 1, sy + sh - 2);

        g2.setColor(SLOT_HIGHLIGHT_LIGHT);
        g2.setStroke(STROKE_1_5F);
        g2.drawLine(sx + 2, sy + sh - 1, sx + sw - 1, sy + sh - 1);
        g2.drawLine(sx + sw - 1, sy + 2, sx + sw - 1, sy + sh - 1);

        g2.setColor(new Color(20, 22, 26, 220));
        g2.setStroke(STROKE_1F);
        g2.drawRoundRect(sx, sy, sw, sh, 6, 6);
    }

    public static void drawSlotHoverOutline(Graphics2D g2, int x, int y, int w, int h) {
        if (w == EnginePanel.tileSquare && h == EnginePanel.tileSquare) {
            if (cachedHoverOutline == null) {
                cachedHoverOutline = createCompatibleImage(w, h);
                Graphics2D bg = cachedHoverOutline.createGraphics();
                bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                bg.setColor(HOVER_BORDER_COLOR);
                bg.setStroke(STROKE_2_5F);
                bg.drawRoundRect(1, 1, w - 2, h - 2, 7, 7);
                bg.dispose();
            }
            g2.drawImage(cachedHoverOutline, x, y, null);
            return;
        }
        g2.setColor(HOVER_BORDER_COLOR);
        g2.setStroke(STROKE_2_5F);
        g2.drawRoundRect(x + 1, y + 1, w - 2, h - 2, 7, 7);
    }

    private static BufferedImage createCompatibleImage(int w, int h) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (!ge.isHeadlessInstance()) {
                return ge.getDefaultScreenDevice().getDefaultConfiguration()
                        .createCompatibleImage(w, h, Transparency.TRANSLUCENT);
            }
        } catch (Exception ignored) {
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
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