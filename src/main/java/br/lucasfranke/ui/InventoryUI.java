package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.item.Item;

import java.awt.*;

public class InventoryUI {

    private static final Color BG_TINT_COLOR = new Color(0, 0, 0, 120);
    private static final Color PANEL_BG_COLOR = new Color(50, 50, 50, 210);
    private static final Color SLOT_BG_COLOR = new Color(200, 200, 200, 180);
    private static final Color SLOT_BORDER_COLOR = new Color(0, 0, 0, 180);
    private static final Color HOVER_COLOR = new Color(255, 255, 255, 200);

    private static final Stroke SLOT_BORDER_STROKE = new BasicStroke(3);
    private static final Stroke HOVER_STROKE = new BasicStroke(6);

    public void draw(Graphics2D g2, Inventory playerInventory, FunctionalBlock functionalBlock, int width, int height) {
        g2.setColor(BG_TINT_COLOR);
        g2.fillRect(0, 0, width, height);

        if (functionalBlock != null) {
            Inventory blockInv = functionalBlock.getBlockInventory();
            Point blockStart = getFunctionalBlockStart(blockInv);
            drawContainerPanel(g2, blockInv, blockStart.x, blockStart.y, functionalBlock.getTitle());

            int panelW = blockInv.getCols() * blockInv.getSlotSize() + 20;
            int panelH = blockInv.getRows() * blockInv.getSlotSize() + 28;
            functionalBlock.drawExtraUI(g2, blockStart.x - 10, blockStart.y - 10, panelW, panelH);

            Point playerStart = getPlayerInventoryStart(playerInventory, true);
            drawContainerPanel(g2, playerInventory, playerStart.x, playerStart.y, "Inventory");
        } else if (playerInventory != null) {
            Point playerStart = getPlayerInventoryStart(playerInventory, false);
            drawContainerPanel(g2, playerInventory, playerStart.x, playerStart.y, "Inventory");
        }
    }

    private void drawContainerPanel(Graphics2D g2, Inventory inventory, int startX, int startY, String title) {
        int slotSize = inventory.getSlotSize();
        int gridW = inventory.getCols() * slotSize;
        int gridH = inventory.getRows() * slotSize;

        g2.setColor(PANEL_BG_COLOR);
        g2.fillRoundRect(startX - 10, startY - 32, gridW + 20, gridH + 50, 15, 15);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 16));
        g2.drawString(title, startX, startY - 10);

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
        if (mouseX >= startX && mouseX < startX + gridW && mouseY >= startY && mouseY < startY + gridH) {
            int col = (mouseX - startX) / slotSize;
            int row = (mouseY - startY) / slotSize;
            g2.setColor(HOVER_COLOR);
            g2.setStroke(HOVER_STROKE);
            g2.drawRect(startX + col * slotSize, startY + row * slotSize, slotSize, slotSize);
        }
    }

    public static Point getPlayerInventoryStart(Inventory inv, boolean hasFunctionalBlockOpen) {
        int startX = (EnginePanel.WIDTH - inv.getCols() * inv.getSlotSize()) / 2;
        int startY = hasFunctionalBlockOpen
                ? 480
                : (EnginePanel.HEIGHT - inv.getRows() * inv.getSlotSize()) / 2;
        return new Point(startX, startY);
    }

    public static Point getFunctionalBlockStart(Inventory blockInv) {
        int startX = (EnginePanel.WIDTH - blockInv.getCols() * blockInv.getSlotSize()) / 2;
        int startY = 180;
        return new Point(startX, startY);
    }
}