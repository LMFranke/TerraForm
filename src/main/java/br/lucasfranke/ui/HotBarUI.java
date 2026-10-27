package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.HotBar;
import br.lucasfranke.model.item.Item;

import java.awt.*;

public class HotBarUI {

    private static final Color BAR_FRAME_BG = new Color(32, 34, 42, 235);
    private static final Color BAR_FRAME_BORDER = new Color(90, 95, 110, 200);
    private static final Color SELECTED_SLOT_BORDER = new Color(255, 220, 90, 245);
    private static final Font KEYBIND_FONT = new Font("SansSerif", Font.BOLD, 10);

    public HotBarUI() {
    }

    public void draw(Graphics2D g2, HotBar hotBar) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int slotSize = EnginePanel.tileSquare;
        int totalSlotsW = hotBar.getCols() * slotSize;
        int startX = (EnginePanel.WIDTH - totalSlotsW) / 2;
        int startY = (int) ((EnginePanel.HEIGHT - hotBar.getRows() * slotSize) * 0.95);

        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillRoundRect(startX - 6, startY - 4, totalSlotsW + 16, slotSize + 14, 14, 14);

        g2.setColor(BAR_FRAME_BG);
        g2.fillRoundRect(startX - 8, startY - 6, totalSlotsW + 16, slotSize + 12, 12, 12);
        g2.setColor(BAR_FRAME_BORDER);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(startX - 8, startY - 6, totalSlotsW + 16, slotSize + 12, 12, 12);

        for (int col = 0; col < hotBar.getCols(); col++) {
            int x = startX + col * slotSize;
            boolean isSelected = (col == hotBar.getSelectedSlot());

            InventoryUI.drawBeveledSlot(g2, x, startY, slotSize, slotSize, isSelected);

            Item item = hotBar.getItem(0, col);
            if (item != null && item.getTileType().getSprite() != null) {
                if (item.getQuantity() <= 0) {
                    hotBar.setItem(0, col, null);
                } else {
                    int offSet = 10;
                    int itemSize = slotSize - offSet;
                    g2.drawImage(item.getSprite(), x + offSet / 2, startY + offSet / 2, itemSize, itemSize, null);
                    drawDurabilityBar(g2, item, x, startY, slotSize);
                }
            }

            g2.setFont(KEYBIND_FONT);
            g2.setColor(isSelected ? new Color(255, 225, 110) : new Color(155, 160, 175, 200));
            g2.drawString(String.valueOf(col + 1), x + 6, startY + 13);
        }

        int selectedX = startX + slotSize * hotBar.getSelectedSlot();
        g2.setColor(SELECTED_SLOT_BORDER);
        g2.setStroke(new BasicStroke(3.5f));
        g2.drawRoundRect(selectedX + 1, startY + 1, slotSize - 2, slotSize - 2, 8, 8);

        Item selected = hotBar.getCurrentItem();
        if (selected != null && selected.getQuantity() > 0) {
            drawSelectedItemLabel(g2, selected, startY);
        }
    }

    private void drawSelectedItemLabel(Graphics2D g2, Item selected, int hotBarY) {
        String label = selected.getTileType().name().replace('_', ' ');
        if (selected.isTool()) {
            label += String.format("  •  %d/%d Dur  •  %.1fx Speed",
                    selected.getDurability(),
                    selected.getMaxDurability(),
                    selected.getTileType().getToolTier().getEfficiencyMultiplier());
        }

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(label);
        int pillW = textW + 24;
        int pillH = 24;
        int pillX = (EnginePanel.WIDTH - pillW) / 2;
        int pillY = hotBarY - 34;

        g2.setColor(new Color(24, 26, 32, 215));
        g2.fillRoundRect(pillX, pillY, pillW, pillH, 12, 12);
        g2.setColor(new Color(85, 90, 105, 180));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(pillX, pillY, pillW, pillH, 12, 12);

        g2.setColor(new Color(245, 245, 250));
        g2.drawString(label, pillX + 12, pillY + 16);
    }

    public static void drawDurabilityBar(Graphics2D g2, Item item, int slotX, int slotY, int slotSize) {
        if (item == null || !item.isTool() || item.getMaxDurability() <= 0) {
            return;
        }
        if (item.getDurability() >= item.getMaxDurability()) {
            return;
        }

        float ratio = item.getDurabilityRatio();
        int barMaxWidth = slotSize - 12;
        int barWidth = Math.max(2, Math.round(barMaxWidth * ratio));
        int barHeight = 4;
        int barX = slotX + 6;
        int barY = slotY + slotSize - 8;

        g2.setColor(new Color(18, 18, 22, 230));
        g2.fillRect(barX, barY, barMaxWidth, barHeight);

        g2.setColor(Color.getHSBColor(ratio * 0.33f, 1.0f, 0.95f));
        g2.fillRect(barX, barY, barWidth, barHeight);
    }
}