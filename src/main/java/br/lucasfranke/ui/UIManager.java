package br.lucasfranke.ui;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.inventory.ItemContainer;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.FunctionalBlock;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.sound.Sound;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class UIManager {

    private record SlotRef(ItemContainer container, int row, int col) {}

    private final HotBarUI hotBarUI;
    private final InventoryUI inventoryUI;
    private Inventory currentlyOpenInventory;
    private FunctionalBlock currentlyOpenFunctionalBlock;
    private final Player player;
    private Item draggedItem;

    private boolean leftMouseWasDown = false;
    private boolean isLeftDragSplitting = false;
    private boolean pickedUpOnThisPress = false;
    private Item dragSplitSourceItem = null;
    private SlotRef dragOriginSlot = null;
    private final List<SlotRef> dragSplitSlots = new ArrayList<>();

    public UIManager(Player player) {
        this.player = player;
        this.hotBarUI = new HotBarUI();
        this.inventoryUI = new InventoryUI();
    }

    public Item getDraggedItem() {
        return draggedItem;
    }

    public void setDraggedItem(Item draggedItem) {
        this.draggedItem = draggedItem;
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
        resetLeftDragState();
        MouseManager.unpressButton(MouseEvent.BUTTON1);
        MouseManager.unpressButton(MouseEvent.BUTTON3);

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
        if (draggedItem != null && draggedItem.getQuantity() > 0) {
            int size = EnginePanel.tileSquare - 12;
            g2.drawImage(draggedItem.getSprite(), MouseManager.getX() - size / 2, MouseManager.getY() - size / 2, size, size, null);
        }
    }

    public void update() {
        if (!isInventoryOpen()) {
            leftMouseWasDown = false;
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

        boolean leftDown = MouseManager.isButtonPressed(MouseEvent.BUTTON1);

        if (leftDown && !leftMouseWasDown) {
            onLeftMousePressed();
        } else if (leftDown && leftMouseWasDown) {
            onLeftMouseDragged();
        } else if (!leftDown && leftMouseWasDown) {
            onLeftMouseReleased();
        }
        leftMouseWasDown = leftDown;

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
            resetLeftDragState();
            if (draggedItem == null) {
                handleRightPickUpHalf();
            } else {
                handleRightPlaceOne();
            }
            MouseManager.unpressButton(MouseEvent.BUTTON3);
        }
    }

    private void onLeftMousePressed() {
        resetLeftDragState();

        if (currentlyOpenFunctionalBlock != null && handleFunctionalExtraClick()) {
            return;
        }

        SlotRef clickedSlot = findClickedSlotRef();
        if (clickedSlot == null) {
            return;
        }

        boolean shiftPressed = KeyManager.isKeyPressed(KeyEvent.VK_SHIFT);
        if (shiftPressed) {
            handleShiftClick(clickedSlot);
            return;
        }

        if (isOutputSlot(clickedSlot)) {
            handleOutputSlotClick(clickedSlot);
            return;
        }

        Item slotItem = clickedSlot.container().getItem(clickedSlot.row(), clickedSlot.col());

        if (draggedItem == null) {
            if (slotItem == null) {
                return;
            }
            draggedItem = slotItem;
            clickedSlot.container().setItem(clickedSlot.row(), clickedSlot.col(), null);

            pickedUpOnThisPress = true;
            dragOriginSlot = clickedSlot;
            dragSplitSourceItem = draggedItem.copyWithQuantity(draggedItem.getQuantity());
        } else {
            if (slotItem == null) {
                if (!draggedItem.isTool() && draggedItem.getQuantity() > 1) {
                    pickedUpOnThisPress = false;
                    dragOriginSlot = clickedSlot;
                    dragSplitSourceItem = draggedItem.copyWithQuantity(draggedItem.getQuantity());
                    isLeftDragSplitting = true;
                    dragSplitSlots.add(clickedSlot);
                    recalculateDragSplit();
                } else {
                    clickedSlot.container().setItem(clickedSlot.row(), clickedSlot.col(), draggedItem);
                    draggedItem = null;
                }
            } else {
                handleLeftDropOnOccupiedSlot(clickedSlot, slotItem);
            }
        }
    }

    private void onLeftMouseDragged() {
        if (dragSplitSourceItem == null || dragSplitSourceItem.isTool() || dragSplitSourceItem.getQuantity() <= 1) {
            return;
        }

        SlotRef hoveredSlot = findClickedSlotRef();
        if (hoveredSlot == null || isOutputSlot(hoveredSlot)) {
            return;
        }

        if (pickedUpOnThisPress && !isLeftDragSplitting) {
            if (!hoveredSlot.equals(dragOriginSlot)
                    && hoveredSlot.container().getItem(hoveredSlot.row(), hoveredSlot.col()) == null) {
                isLeftDragSplitting = true;
                if (dragOriginSlot != null
                        && !isOutputSlot(dragOriginSlot)
                        && dragOriginSlot.container().getItem(dragOriginSlot.row(), dragOriginSlot.col()) == null) {
                    dragSplitSlots.add(dragOriginSlot);
                }
            }
        }

        if (isLeftDragSplitting) {
            if (!dragSplitSlots.contains(hoveredSlot)
                    && hoveredSlot.container().getItem(hoveredSlot.row(), hoveredSlot.col()) == null
                    && dragSplitSlots.size() < dragSplitSourceItem.getQuantity()) {
                dragSplitSlots.add(hoveredSlot);
                recalculateDragSplit();
            }
        }
    }

    private void onLeftMouseReleased() {
        resetLeftDragState();
    }

    private void recalculateDragSplit() {
        if (dragSplitSourceItem == null || dragSplitSlots.isEmpty()) {
            return;
        }

        int total = dragSplitSourceItem.getQuantity();
        int slotCount = dragSplitSlots.size();
        int perSlot = total / slotCount;
        int remainder = total % slotCount;

        for (SlotRef slot : dragSplitSlots) {
            if (perSlot > 0) {
                slot.container().setItem(slot.row(), slot.col(), dragSplitSourceItem.copyWithQuantity(perSlot));
            } else {
                slot.container().setItem(slot.row(), slot.col(), null);
            }
        }

        if (remainder > 0) {
            draggedItem = dragSplitSourceItem.copyWithQuantity(remainder);
        } else {
            draggedItem = null;
        }
    }

    private void resetLeftDragState() {
        isLeftDragSplitting = false;
        pickedUpOnThisPress = false;
        dragSplitSourceItem = null;
        dragOriginSlot = null;
        dragSplitSlots.clear();
    }

    private boolean isOutputSlot(SlotRef slot) {
        return currentlyOpenFunctionalBlock != null
                && slot.container() == currentlyOpenFunctionalBlock.getBlockInventory()
                && currentlyOpenFunctionalBlock.isOutputSlot(slot.row(), slot.col());
    }

    private void handleOutputSlotClick(SlotRef outputSlot) {
        Item slotItem = outputSlot.container().getItem(outputSlot.row(), outputSlot.col());
        if (slotItem == null || slotItem.getQuantity() <= 0) {
            return;
        }

        if (draggedItem == null) {
            draggedItem = slotItem;
            outputSlot.container().setItem(outputSlot.row(), outputSlot.col(), null);
        } else if (!slotItem.isTool()
                && draggedItem.getTileType() == slotItem.getTileType()
                && draggedItem.getQuantity() < draggedItem.getMaxStackSize()) {
            int space = draggedItem.getMaxStackSize() - draggedItem.getQuantity();
            int toTransfer = Math.min(space, slotItem.getQuantity());
            draggedItem.increaseQuantity(toTransfer);
            slotItem.setQuantity(slotItem.getQuantity() - toTransfer);
            if (slotItem.getQuantity() <= 0) {
                outputSlot.container().setItem(outputSlot.row(), outputSlot.col(), null);
            }
        }
    }

    private void handleShiftClick(SlotRef clickedSlot) {
        Item slotItem = clickedSlot.container().getItem(clickedSlot.row(), clickedSlot.col());
        if (slotItem == null || slotItem.getQuantity() <= 0) {
            return;
        }

        int initialQty = slotItem.getQuantity();
        ItemContainer source = clickedSlot.container();

        if (currentlyOpenFunctionalBlock != null && source == currentlyOpenFunctionalBlock.getBlockInventory()) {
            transferToContainer(player.hotBar, slotItem);
            if (slotItem.getQuantity() > 0) {
                transferToContainer(player.inventory, slotItem);
            }
        } else {
            if (currentlyOpenFunctionalBlock != null && currentlyOpenFunctionalBlock.isWorldBlock()) {
                currentlyOpenFunctionalBlock.tryShiftClickInsert(slotItem);
            } else {
                if (source == player.inventory) {
                    transferToContainer(player.hotBar, slotItem);
                } else if (source == player.hotBar) {
                    transferToContainer(player.inventory, slotItem);
                }
            }
        }

        if (slotItem.getQuantity() <= 0) {
            source.setItem(clickedSlot.row(), clickedSlot.col(), null);
        }
        if (slotItem.getQuantity() < initialQty) {
            EnginePanel.soundManager.playSound(Sound.ITEM_PICKUP);
        }
    }

    private void transferToContainer(ItemContainer target, Item sourceItem) {
        if (sourceItem == null || sourceItem.getQuantity() <= 0) {
            return;
        }

        if (!sourceItem.isTool()) {
            for (int r = 0; r < target.getRows(); r++) {
                for (int c = 0; c < target.getCols(); c++) {
                    Item dest = target.getItem(r, c);
                    if (dest != null && dest.getTileType() == sourceItem.getTileType()
                            && dest.getQuantity() < dest.getMaxStackSize()) {
                        int space = dest.getMaxStackSize() - dest.getQuantity();
                        int toMove = Math.min(space, sourceItem.getQuantity());
                        dest.increaseQuantity(toMove);
                        sourceItem.setQuantity(sourceItem.getQuantity() - toMove);
                        if (sourceItem.getQuantity() <= 0) {
                            return;
                        }
                    }
                }
            }
        }

        for (int r = 0; r < target.getRows(); r++) {
            for (int c = 0; c < target.getCols(); c++) {
                if (target.getItem(r, c) == null) {
                    int toPlace = Math.min(sourceItem.getMaxStackSize(), sourceItem.getQuantity());
                    target.setItem(r, c, sourceItem.copyWithQuantity(toPlace));
                    sourceItem.setQuantity(sourceItem.getQuantity() - toPlace);
                    if (sourceItem.getQuantity() <= 0) {
                        return;
                    }
                }
            }
        }
    }

    private void handleLeftDropOnOccupiedSlot(SlotRef slot, Item destItem) {
        int maxStack = destItem.getMaxStackSize();
        if (destItem.getTileType() == draggedItem.getTileType() && maxStack > 1) {
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

        slot.container().setItem(slot.row(), slot.col(), draggedItem);
        draggedItem = destItem;
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

    private SlotRef findClickedSlotRef() {
        if (currentlyOpenFunctionalBlock != null) {
            Point blockStart = InventoryUI.getFunctionalBlockStart(currentlyOpenFunctionalBlock);
            Point loc = getPointAtFunctionalBlock(currentlyOpenFunctionalBlock, blockStart.x, blockStart.y);
            if (loc != null) {
                return new SlotRef(currentlyOpenFunctionalBlock.getBlockInventory(), loc.y, loc.x);
            }
        }

        if (currentlyOpenInventory != null) {
            Point invStart = InventoryUI.getPlayerInventoryStart(currentlyOpenInventory, currentlyOpenFunctionalBlock != null);
            Point loc = getPointAtContainer(currentlyOpenInventory, invStart.x, invStart.y);
            if (loc != null) {
                return new SlotRef(currentlyOpenInventory, loc.y, loc.x);
            }
        }

        Point hotbarLoc = getSlotAtHotbar();
        if (hotbarLoc != null) {
            return new SlotRef(player.hotBar, hotbarLoc.y, hotbarLoc.x);
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

    private void handleRightPickUpHalf() {
        SlotRef slot = findClickedSlotRef();
        if (slot == null) {
            return;
        }

        Item slotItem = slot.container().getItem(slot.row(), slot.col());
        if (slotItem == null) {
            return;
        }

        int half = (slotItem.getQuantity() + 1) / 2;
        int remaining = slotItem.getQuantity() - half;

        draggedItem = slotItem.copyWithQuantity(half);

        if (remaining <= 0) {
            slot.container().setItem(slot.row(), slot.col(), null);
        } else {
            slotItem.setQuantity(remaining);
        }
    }

    private void handleRightPlaceOne() {
        SlotRef slot = findClickedSlotRef();
        if (slot == null || isOutputSlot(slot)) {
            return;
        }

        Item destItem = slot.container().getItem(slot.row(), slot.col());

        if (destItem == null) {
            slot.container().setItem(slot.row(), slot.col(), draggedItem.copyWithQuantity(1));
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