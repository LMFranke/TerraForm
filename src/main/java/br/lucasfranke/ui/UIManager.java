package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.inventory.ItemContainer;
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
        if (draggedItem != null) {
            player.addItem(draggedItem.getTileType(), draggedItem.getQuantity());
            draggedItem = null;
        }
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
            int size = EnginePanel.tileSquare - 12;
            g2.drawImage(draggedItem.getSprite(), MouseManager.getX() - size / 2, MouseManager.getY() - size / 2, size, size, null);
        }
    }

    public void update() {
        if (!isInventoryOpen()) {
            return;
        }

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
            if (draggedItem == null) {
                handleLeftPickUp();
            } else {
                handleLeftDrop();
            }
            MouseManager.unpressButton(MouseEvent.BUTTON1);
        } else if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
            if (draggedItem == null) {
                handleRightPickUpHalf();
            } else {
                handleRightPlaceOne();
            }
            MouseManager.unpressButton(MouseEvent.BUTTON3);
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

    private void handleLeftPickUp() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) return;

        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item itemToDrag = container.getItem(loc.y, loc.x);
        if (itemToDrag == null) return;

        draggedItem = itemToDrag;
        container.setItem(loc.y, loc.x, null);
    }

    private void handleLeftDrop() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) return;

        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item destItem = container.getItem(loc.y, loc.x);

        if (destItem != null && destItem.getTileType() == draggedItem.getTileType()) {
            int space = Player.MAX_STACK_SIZE - destItem.getQuantity();
            if (space > 0) {
                int toTransfer = Math.min(space, draggedItem.getQuantity());
                destItem.increaseQuantity(toTransfer);
                draggedItem.setQuantity(draggedItem.getQuantity() - toTransfer);
                if (draggedItem.getQuantity() <= 0) {
                    draggedItem = null;
                }
            }
            return;
        }

        container.setItem(loc.y, loc.x, draggedItem);
        draggedItem = destItem;
    }

    private void handleRightPickUpHalf() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) return;

        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item slotItem = container.getItem(loc.y, loc.x);
        if (slotItem == null) return;

        int half = (slotItem.getQuantity() + 1) / 2;
        int remaining = slotItem.getQuantity() - half;

        draggedItem = new Item(slotItem.getTileType(), half);
        if (remaining <= 0) {
            container.setItem(loc.y, loc.x, null);
        } else {
            slotItem.setQuantity(remaining);
        }
    }

    private void handleRightPlaceOne() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) return;

        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item destItem = container.getItem(loc.y, loc.x);

        if (destItem == null) {
            container.setItem(loc.y, loc.x, new Item(draggedItem.getTileType(), 1));
            draggedItem.decreaseQuantity();
        } else if (destItem.getTileType() == draggedItem.getTileType() && destItem.getQuantity() < Player.MAX_STACK_SIZE) {
            destItem.increaseQuantity();
            draggedItem.decreaseQuantity();
        } else {
            return;
        }

        if (draggedItem.getQuantity() <= 0) {
            draggedItem = null;
        }
    }

    private Point getSlotAtInventory() {
        if (currentlyOpenInventory == null) {
            return null;
        }
        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        int slotSize = currentlyOpenInventory.getSlotSize();

        int inventoryStartX = (EnginePanel.WIDTH - currentlyOpenInventory.getCols() * slotSize) / 2;
        int inventoryStartY = (EnginePanel.HEIGHT - currentlyOpenInventory.getRows() * slotSize) / 2;
        int inventoryEndX = inventoryStartX + slotSize * currentlyOpenInventory.getCols();
        int inventoryEndY = inventoryStartY + slotSize * currentlyOpenInventory.getRows();

        return getPointAtSlots(mouseX, mouseY, slotSize, inventoryStartX, inventoryStartY, inventoryEndX, inventoryEndY);
    }

    private Point getSlotAtHotbar() {
        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        int slotSize = EnginePanel.tileSquare;

        int hotbarStartX = (EnginePanel.WIDTH - player.hotBar.getCols() * slotSize) / 2;
        int hotbarStartY = (int) ((EnginePanel.HEIGHT - player.hotBar.getRows() * slotSize) * 0.95);
        int hotbarEndX = hotbarStartX + slotSize * player.hotBar.getCols();
        int hotbarEndY = hotbarStartY + slotSize * player.hotBar.getRows();

        return getPointAtSlots(mouseX, mouseY, slotSize, hotbarStartX, hotbarStartY, hotbarEndX, hotbarEndY);
    }

    private Point getPointAtSlots(int mouseX, int mouseY, int slotSize, int startX, int startY, int endX, int endY) {
        if (mouseX >= startX && mouseX < endX && mouseY >= startY && mouseY < endY) {
            int col = (mouseX - startX) / slotSize;
            int row = (mouseY - startY) / slotSize;
            return new Point(col, row);
        }
        return null;
    }
}