package br.lucasfranke.inventory;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.item.Item;

import java.awt.event.KeyEvent;

public class HotBar implements ItemContainer {

    private final int rows = 1;
    private final int cols = 9;

    private final Item[][] slots;
    private Item currentItem;
    private int selectedSlot = 0;
    private final int slotSize = EnginePanel.tileSquare / 2;

    public HotBar() {
        slots = new Item[rows][cols];
    }

    @Override
    public Item getItem(int row, int col) {
        return slots[row][col];
    }

    @Override
    public void setItem(int row, int col, Item item) {
        slots[row][col] = item;
    }

    @Override
    public int getRows() {
        return rows;
    }

    @Override
    public int getCols() {
        return cols;
    }

    @Override
    public int getSlotSize() {
        return slotSize;
    }

    public int getSelectedSlot() {
        return selectedSlot;
    }

    public Item getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Item currentItem) {
        this.currentItem = currentItem;
    }

    public void update() {

        for (int keyCode = KeyEvent.VK_1; keyCode <= KeyEvent.VK_9; keyCode++) {
            if (KeyManager.isKeyPressed(keyCode)) {
                selectedSlot = keyCode - KeyEvent.VK_1;
                KeyManager.unpressKey(keyCode);
                break;
            }
        }

        if (MouseManager.getWheelRotation() > 0) {
            selectedSlot = selectedSlot == 8 ? 0 : selectedSlot + 1;
            MouseManager.setWheelRotation(0);
        }

        if (MouseManager.getWheelRotation() < 0) {
            selectedSlot = selectedSlot == 0 ? 8 : selectedSlot - 1;
            MouseManager.setWheelRotation(0);
        }

        currentItem = slots[0][selectedSlot];

    }

}
