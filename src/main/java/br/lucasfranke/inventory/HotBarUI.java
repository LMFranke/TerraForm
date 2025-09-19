package br.lucasfranke.inventory;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.item.Item;

import java.awt.*;

public class HotBarUI {

    public HotBarUI() {

    }

    public void draw(Graphics2D g2, HotBar hotBar) {
        int slotSize = EnginePanel.tileSquare;
        int startX = (EnginePanel.WIDTH - hotBar.getCols() * slotSize) / 2;
        int startY = (int) ((EnginePanel.HEIGHT - hotBar.getRows() * slotSize) * 0.95);

        for (int col = 0; col < hotBar.getCols(); col++) {
            int x = startX + col * slotSize;

            g2.setColor(new Color(200, 200, 200, 180));
            g2.fillRect(x, startY, slotSize, slotSize);

            g2.setColor(new Color(0, 0, 0, 230));
            g2.setStroke(new BasicStroke(3));
            g2.drawRect(x, startY, slotSize, slotSize);

            Item item = hotBar.getItem(0, col);
            if (item != null && item.getTileType().getSprite() != null) {
                int offSet = 12;
                int itemSize = slotSize - offSet;
                g2.drawImage(item.getSprite(), x + offSet / 2, startY + offSet / 2, itemSize, itemSize, null);
            }

        }

        int col = startX + slotSize * hotBar.getSelectedSlot();

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setStroke(new BasicStroke(6));

        g2.drawRect(col, startY, slotSize, slotSize);
    }

}
