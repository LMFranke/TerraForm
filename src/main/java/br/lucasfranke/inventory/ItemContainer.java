package br.lucasfranke.inventory;

import br.lucasfranke.model.item.Item;

public interface ItemContainer {
    Item getItem(int row, int col);
    void setItem(int row, int col, Item item);
    int getRows();
    int getCols();
    int getSlotSize();
}
