package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.item.Item;

import java.awt.*;

public class InventoryUI {

    // Colors
    private static final Color BG_TINT_COLOR = new Color(0, 0, 0, 120);
    private static final Color PANEL_BG_COLOR = new Color(50, 50, 50, 200);
    private static final Color SLOT_BG_COLOR = new Color(200, 200, 200, 180);
    private static final Color SLOT_BORDER_COLOR = new Color(0, 0, 0, 180);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 200);

    private static final Stroke SLOT_BORDER_STROKE = new BasicStroke(3);
    private static final Stroke HOVER_STROKE = new BasicStroke(6);

    public InventoryUI() {

    }

    public void draw(Graphics2D g2, Inventory inventory, int width, int height) {
        int startX = (EnginePanel.WIDTH - inventory.getCols() * inventory.getSlotSize()) / 2;
        int startY = (EnginePanel.HEIGHT - inventory.getRows() * inventory.getSlotSize()) / 2;

        int slotSize = inventory.getSlotSize();

        g2.setColor(BG_TINT_COLOR);
        g2.fillRect(0, 0, width, height);

        g2.setColor(PANEL_BG_COLOR);
        g2.fillRoundRect(startX - 10, startY - 10,
                inventory.getCols() * slotSize + 20,
                inventory.getRows() * slotSize + 20,
                15, 15);

        g2.setColor(SLOT_BORDER_COLOR);
        g2.setStroke(SLOT_BORDER_STROKE);

        for (int row = 0; row < inventory.getRows(); row++) {
            for (int col = 0; col < inventory.getCols(); col++) {
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;

                g2.setColor(SLOT_BG_COLOR);
                g2.fillRect(x, y, slotSize, slotSize);

                g2.setColor(SLOT_BORDER_COLOR);
                g2.drawRect(x, y, slotSize, slotSize);

                Item item = inventory.getItem(row, col);
                if (item != null && item.getTileType().getSprite() != null) {
                    if (item.getQuantity() <= 0) {
                        inventory.setItem(row, col, null);
                    } else {
                        int offSet = 12;
                        int itemSize = slotSize - offSet;
                        g2.drawImage(item.getSprite(), x + offSet / 2, y + offSet / 2, itemSize, itemSize, null);
                    }
                }
            }
        }

        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();

        int endX = startX + slotSize * inventory.getCols();
        int endY = startY + slotSize * inventory.getRows();

        if (mouseX >= startX && mouseX <= endX && mouseY >= startY && mouseY <= endY) {
            int col = (mouseX - startX) / slotSize;
            int row = (mouseY - startY) / slotSize;

            int highlightX = startX + col * slotSize;
            int highlightY = startY + row * slotSize;

            g2.setColor(HOVER_COLOR);
            g2.setStroke(HOVER_STROKE);
            g2.drawRect(highlightX, highlightY, slotSize, slotSize);
        }
    }
}