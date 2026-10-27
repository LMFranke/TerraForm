package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.inventory.ItemContainer;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;

import java.awt.*;
import java.awt.event.MouseEvent;

public class UIManager {

    private final HotBarUI hotBarUI;
    private final InventoryUI inventoryUI;
    private Inventory currentlyOpenInventory;
    private FunctionalBlock currentlyOpenFunctionalBlock;
    private final Player player;

    private Item draggedItem;

    public UIManager(Player player) {
        this.player = player;
        this.hotBarUI = new HotBarUI();
        this.inventoryUI = new InventoryUI();
    }

    public void toggleInventory(Inventory inventoryToOpen) {
        if (isInventoryOpen()) {
            closeInventory();
        } else {
            this.currentlyOpenInventory = inventoryToOpen;
            this.currentlyOpenFunctionalBlock = player.personalCrafting;
        }
    }

    public void openFunctionalBlock(FunctionalBlock block) {
        this.currentlyOpenFunctionalBlock = block;
        this.currentlyOpenInventory = player.inventory;
    }

    public FunctionalBlock getCurrentlyOpenFunctionalBlock() {
        return currentlyOpenFunctionalBlock;
    }

    public void closeInventory() {
        if (draggedItem != null) {
            player.addItem(draggedItem);
            draggedItem = null;
        }

        if (currentlyOpenFunctionalBlock != null && !currentlyOpenFunctionalBlock.isWorldBlock()) {
            currentlyOpenFunctionalBlock.returnItemsToPlayer(player);
        }
        currentlyOpenInventory = null;
        currentlyOpenFunctionalBlock = null;
    }

    public boolean isInventoryOpen() {
        return currentlyOpenInventory != null || currentlyOpenFunctionalBlock != null;
    }

    public void draw(Graphics2D g2, int panelWidth, int panelHeight) {
        if (isInventoryOpen()) {
            inventoryUI.draw(g2, currentlyOpenInventory, currentlyOpenFunctionalBlock, panelWidth, panelHeight);
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

        if (currentlyOpenFunctionalBlock != null && currentlyOpenFunctionalBlock.isWorldBlock()) {
            int playerTileX = (int) (player.worldX / EnginePanel.tileSquare);
            int playerTileY = (int) (player.worldY / EnginePanel.tileSquare);
            int dist = Math.abs(currentlyOpenFunctionalBlock.getWorldX() - playerTileX)
                    + Math.abs(currentlyOpenFunctionalBlock.getWorldY() - playerTileY);
            if (dist >= 5) {
                closeInventory();
                return;
            }
        }

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
            if (currentlyOpenFunctionalBlock != null && handleFunctionalExtraClick()) {
                MouseManager.unpressButton(MouseEvent.BUTTON1);
                return;
            }
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

    private boolean handleFunctionalExtraClick() {
        Dimension dim = currentlyOpenFunctionalBlock.getContentDimension();
        Point start = InventoryUI.getFunctionalBlockStart(currentlyOpenFunctionalBlock);
        int panelW = dim.width + 20;
        int panelH = dim.height + 28;
        return currentlyOpenFunctionalBlock.handleExtraUIClick(
                MouseManager.getX(), MouseManager.getY(), player,
                start.x - 10, start.y - 10, panelW, panelH
        );
    }

    private Object[] findClickedSlot() {
        if (currentlyOpenFunctionalBlock != null) {
            Point blockStart = InventoryUI.getFunctionalBlockStart(currentlyOpenFunctionalBlock);
            Point loc = getPointAtFunctionalBlock(currentlyOpenFunctionalBlock, blockStart.x, blockStart.y);
            if (loc != null) {
                return new Object[]{loc, currentlyOpenFunctionalBlock.getBlockInventory()};
            }
        }
        if (currentlyOpenInventory != null) {
            Point invStart = InventoryUI.getPlayerInventoryStart(currentlyOpenInventory, currentlyOpenFunctionalBlock != null);
            Point loc = getPointAtContainer(currentlyOpenInventory, invStart.x, invStart.y);
            if (loc != null) {
                return new Object[]{loc, currentlyOpenInventory};
            }
        }
        Point hotbarLoc = getSlotAtHotbar();
        if (hotbarLoc != null) {
            return new Object[]{hotbarLoc, player.hotBar};
        }
        return null;
    }

    private Point getPointAtFunctionalBlock(FunctionalBlock fb, int startX, int startY) {
        int mouseX = MouseManager.getX();
        int mouseY = MouseManager.getY();
        Inventory inv = fb.getBlockInventory();
        for (int row = 0; row < inv.getRows(); row++) {
            for (int col = 0; col < inv.getCols(); col++) {
                if (fb.getSlotBounds(row, col, startX, startY).contains(mouseX, mouseY)) {
                    return new Point(col, row);
                }
            }
        }
        return null;
    }

    private void handleLeftPickUp() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) {
            return;
        }
        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item itemToDrag = container.getItem(loc.y, loc.x);
        if (itemToDrag == null) {
            return;
        }
        draggedItem = itemToDrag;
        container.setItem(loc.y, loc.x, null);
    }

    private void handleLeftDrop() {
        Object[] slotInfo = findClickedSlot();
        if (slotInfo == null) {
            return;
        }
        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item destItem = container.getItem(loc.y, loc.x);
        int maxStack = destItem != null ? destItem.getMaxStackSize() : Player.MAX_STACK_SIZE;

        if (destItem != null && destItem.getTileType() == draggedItem.getTileType() && maxStack > 1) {
            int space = maxStack - destItem.getQuantity();
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
        if (slotInfo == null) {
            return;
        }
        Point loc = (Point) slotInfo[0];
        ItemContainer container = (ItemContainer) slotInfo[1];

        Item slotItem = container.getItem(loc.y, loc.x);
        if (slotItem == null) {
            return;
        }

        int half = (slotItem.getQuantity() + 1) / 2;
        int remaining = slotItem.getQuantity() - half;

        draggedItem = slotItem.copyWithQuantity(half);
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
            container.setItem(loc.y, loc.x, draggedItem.copyWithQuantity(1));
            draggedItem.decreaseQuantity();
        } else if (destItem.getTileType() == draggedItem.getTileType() && destItem.getQuantity() < destItem.getMaxStackSize()) {
            destItem.increaseQuantity();
            draggedItem.decreaseQuantity();
        } else {
            return;
        }

        if (draggedItem.getQuantity() <= 0) {
            draggedItem = null;
        }
    }

    private Point getPointAtContainer(Inventory inv, int startX, int startY) {
        int slotSize = inv.getSlotSize();
        int endX = startX + slotSize * inv.getCols();
        int endY = startY + slotSize * inv.getRows();
        return getPointAtSlots(MouseManager.getX(), MouseManager.getY(), slotSize, startX, startY, endX, endY);
    }

    private Point getSlotAtHotbar() {
        int slotSize = EnginePanel.tileSquare;
        int hotbarStartX = (EnginePanel.WIDTH - player.hotBar.getCols() * slotSize) / 2;
        int hotbarStartY = (int) ((EnginePanel.HEIGHT - player.hotBar.getRows() * slotSize) * 0.95);
        int hotbarEndX = hotbarStartX + slotSize * player.hotBar.getCols();
        int hotbarEndY = hotbarStartY + slotSize * player.hotBar.getRows();
        return getPointAtSlots(MouseManager.getX(), MouseManager.getY(), slotSize, hotbarStartX, hotbarStartY, hotbarEndX, hotbarEndY);
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