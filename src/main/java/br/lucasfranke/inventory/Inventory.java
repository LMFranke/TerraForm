package br.lucasfranke.inventory;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;

public class Inventory implements ItemContainer {
    private final int rows;
    private final int cols;
    private int slotSize = EnginePanel.tileSquare;

    private Item[][] slots;

    public Inventory(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        slots = new Item[rows][cols];

        slots[0][0] = new Item(TileType.GRASS, 64);
        slots[3][5] = new Item(TileType.STONE, 48);
        slots[4][0] = new Item(TileType.STONE, 48);
    }

    public Inventory(int rows, int cols, int slotSize) {
        this.slotSize = slotSize;
        this.rows = rows;
        this.cols = cols;

        slots = new Item[rows][cols];

        slots[0][0] = new Item(TileType.GRASS, 64);
        slots[3][5] = new Item(TileType.STONE, 48);
    }

    public void update() {

//        if (!EnginePanel.uiManager.isInventoryOpen()) {
//            return;
//        }
//
//        int x = MouseManager.getX();
//        int y = MouseManager.getY();
//
//        int row = (y - inventoryUI.getStartY()) / slotSize;
//        int col = (x - inventoryUI.getStartX()) / slotSize;
//
//        if (row < 0 || col < 0 || row >= rows || col >= cols || x < getInventoryUI().getStartX() || y < getInventoryUI().getStartY()) {
//            return;
//        }
//
//        Item selectedItem = slots[row][col];
//        if (selectedItem != null) {
//            selectedItem.setQuantity(selectedItem.getQuantity() + 1);
//        }

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

}
