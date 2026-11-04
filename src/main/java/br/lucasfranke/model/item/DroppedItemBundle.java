package br.lucasfranke.model.item;

import br.lucasfranke.model.type.TileType;

import java.util.ArrayList;
import java.util.List;

public class DroppedItemBundle {

    private final List<Item> items = new ArrayList<>();
    private boolean forcedLootBag = false;

    public DroppedItemBundle() {
    }

    public DroppedItemBundle(Item initialItem) {
        add(initialItem);
    }

    public DroppedItemBundle(List<Item> initialItems) {
        if (initialItems != null) {
            for (Item item : initialItems) {
                add(item);
            }
        }
        if (this.items.size() > 1) {
            this.forcedLootBag = true;
        }
    }

    public void add(Item newItem) {
        if (newItem == null || newItem.getTileType() == null
                || newItem.getTileType() == TileType.AIR || newItem.getQuantity() <= 0) {
            return;
        }

        Item copy = newItem.copyWithQuantity(newItem.getQuantity());

        if (!copy.isTool()) {
            for (Item existing : items) {
                if (existing.getTileType() == copy.getTileType()
                        && existing.getQuantity() < existing.getMaxStackSize()) {
                    int space = existing.getMaxStackSize() - existing.getQuantity();
                    int toTransfer = Math.min(space, copy.getQuantity());
                    existing.increaseQuantity(toTransfer);
                    copy.setQuantity(copy.getQuantity() - toTransfer);
                    if (copy.getQuantity() <= 0) {
                        break;
                    }
                }
            }
        }

        while (copy.getQuantity() > 0) {
            int stackQty = Math.min(copy.getMaxStackSize(), copy.getQuantity());
            items.add(copy.copyWithQuantity(stackQty));
            copy.setQuantity(copy.getQuantity() - stackQty);
        }

        if (items.size() > 1) {
            forcedLootBag = true;
        }
    }

    public void absorb(DroppedItemBundle other) {
        if (other == null || other == this || other.isEmpty()) {
            return;
        }
        for (Item item : other.items) {
            add(item);
        }
        other.clear();
    }

    public boolean canMergeAsSingleStackWith(DroppedItemBundle other) {
        if (other == null || this.isLootBag() || other.isLootBag()) {
            return false;
        }
        Item a = this.getPrimaryItem();
        Item b = other.getPrimaryItem();
        if (a == null || b == null || a.isTool() || b.isTool()) {
            return false;
        }
        return a.getTileType() == b.getTileType()
                && (a.getQuantity() + b.getQuantity() <= a.getMaxStackSize());
    }

    public void forceLootBagMode() {
        this.forcedLootBag = true;
    }

    public void refreshModeAfterCollection() {
        if (items.size() <= 1) {
            forcedLootBag = false;
        }
    }

    public boolean isLootBag() {
        return forcedLootBag || items.size() > 1;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
    }

    public List<Item> getItems() {
        return items;
    }

    public Item getPrimaryItem() {
        return items.isEmpty() ? null : items.getFirst();
    }

    public int getTotalQuantity() {
        int total = 0;
        for (Item item : items) {
            total += item.getQuantity();
        }
        return total;
    }
}