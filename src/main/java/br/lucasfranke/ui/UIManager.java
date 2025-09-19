package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.HotBar;
import br.lucasfranke.inventory.HotBarUI;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.inventory.InventoryUI;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;

import java.awt.*;
import java.awt.event.MouseEvent;

public class UIManager {

    private final HotBarUI hotBarUI;
    private final InventoryUI inventoryUI;

    private Inventory currentlyOpenInventory;
    private final Player player;
    private Item draggedItem;

    public UIManager(Player player) {
        this.player = player;
        this.hotBarUI = new HotBarUI();
        this.inventoryUI = new InventoryUI();
        this.currentlyOpenInventory = null;
    }

    public void toggleInventory(Inventory inventoryToOpen) {
        if (currentlyOpenInventory == inventoryToOpen) {
            closeInventory();
        } else {
            currentlyOpenInventory = inventoryToOpen;
        }
    }

    public void closeInventory() {
        currentlyOpenInventory = null;
    }

    public boolean isInventoryOpen() {
        return currentlyOpenInventory != null;
    }

    public void draw(Graphics2D g2, int panelWidth, int panelHeight) {
        if (currentlyOpenInventory != null) {
            inventoryUI.draw(g2, currentlyOpenInventory, panelWidth, panelHeight);
        }
        hotBarUI.draw(g2, player.hotBar);

        if (draggedItem != null) {
            int offsetX = player.inventory.getSlotSize() / 2;
            int offsetY = player.inventory.getSlotSize() / 2;
            g2.drawImage(draggedItem.getSprite(), MouseManager.getX() - offsetX, MouseManager.getY() - offsetY, null);
        }

    }

    public void update() {
        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
            if (draggedItem == null) {
                handlePickUp();
            } else {
                handleDrop();
            }
            MouseManager.unpressButton(MouseEvent.BUTTON1);
        }
    }

    private Object[] findClickedSlot() {
        Point location = getSlotAtInventory();
        if (location != null) {
            return new Object[]{location, currentlyOpenInventory};
        }

        location = getSlotAtHotbar();
        if (location != null) {
            return new Object[]{location, player.hotBar};
        }

        return null;
    }

    private void handlePickUp() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) {
            return;
        }

        Point location = (Point) slotInfo[0];
        Object container = slotInfo[1];

        Item itemToDrag = (container instanceof Inventory)
                ? ((Inventory) container).getItem(location.y, location.x)
                : ((HotBar) container).getItem(location.y, location.x);

        if (itemToDrag == null) {
            return;
        }

        draggedItem = itemToDrag;

        if (container instanceof Inventory) {
            ((Inventory) container).setItem(location.y, location.x, null);
        } else {
            ((HotBar) container).setItem(location.y, location.x, null);
        }
    }

    private void handleDrop() {
        Object[] slotInfo = findClickedSlot();

        if (slotInfo == null) {
            draggedItem = null;
            return;
        }

        Point location = (Point) slotInfo[0];
        Object container = slotInfo[1];

        Item itemInDestination = (container instanceof Inventory)
                ? ((Inventory) container).getItem(location.y, location.x)
                : ((HotBar) container).getItem(location.y, location.x);

        if (container instanceof Inventory) {
            ((Inventory) container).setItem(location.y, location.x, draggedItem);
        } else {
            ((HotBar) container).setItem(location.y, location.x, draggedItem);
        }

        draggedItem = itemInDestination;
    }

    private Point getSlotAtInventory() {
        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        int slotSize = currentlyOpenInventory.getSlotSize();

        int inventoryStartX = (EnginePanel.WIDTH - currentlyOpenInventory.getCols() * currentlyOpenInventory.getSlotSize()) / 2;
        int inventoryStartY = (EnginePanel.HEIGHT - currentlyOpenInventory.getRows() * currentlyOpenInventory.getSlotSize()) / 2;

        int inventoryEndX = inventoryStartX + slotSize * currentlyOpenInventory.getCols();
        int inventoryEndY = inventoryStartY + slotSize * currentlyOpenInventory.getRows();

        return getPointAtSlots(mouseX, mouseY, slotSize, inventoryStartX, inventoryStartY, inventoryEndX, inventoryEndY);
    }

    private Point getSlotAtHotbar() {
        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        int slotSize = currentlyOpenInventory.getSlotSize();

        int hotbarStartX = (EnginePanel.WIDTH - player.hotBar.getCols() * slotSize) / 2;
        int hotbarStartY = (int) ((EnginePanel.HEIGHT - player.hotBar.getRows() * slotSize) * 0.95);

        int hotbarEndX = hotbarStartX + slotSize * player.hotBar.getCols();
        int hotbarEndY = hotbarStartY + slotSize * player.hotBar.getRows();

        return getPointAtSlots(mouseX, mouseY, slotSize, hotbarStartX, hotbarStartY, hotbarEndX, hotbarEndY);
    }

    private Point getPointAtSlots(int mouseX, int mouseY, int slotSize, int inventoryStartX, int inventoryStartY, int inventoryEndX, int inventoryEndY) {
        if (mouseX >= inventoryStartX && mouseX <= inventoryEndX && mouseY >= inventoryStartY && mouseY <= inventoryEndY) {
            int col = (mouseX - inventoryStartX) / slotSize;
            int row = (mouseY - inventoryStartY) / slotSize;
            return new Point(col, row);
        }

        return null;
    }

}
