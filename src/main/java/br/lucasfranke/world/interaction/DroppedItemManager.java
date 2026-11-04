package br.lucasfranke.world.interaction;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.model.entity.DroppedItem;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.model.item.DroppedItemBundle;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.World;
import br.lucasfranke.world.render.WorldRenderer;
import br.lucasfranke.world.render.DroppedItemRenderer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class DroppedItemManager {

    private static final double MAGNET_RADIUS_PIXELS = EnginePanel.tileSquare * 1.65;
    private static final double ITEM_MERGE_RADIUS_PIXELS = EnginePanel.tileSquare * 1.35;
    private static final int LOOT_BAG_CLUSTER_THRESHOLD = 3;
    private static final int ITEM_MERGE_CHECK_INTERVAL = 12;

    private final World world;
    private final DroppedItemRenderer renderer = new DroppedItemRenderer();
    private final Random random = new Random();
    private final List<DroppedItem> droppedItems = new CopyOnWriteArrayList<>();

    private int mergeTickCounter = 0;

    public DroppedItemManager(World world) {
        this.world = world;
    }

    public void spawnDroppedItem(double worldX, double worldY, Item item, double vx, double vy, int pickupDelay) {
        if (item == null || item.getTileType() == null
                || item.getTileType() == TileType.AIR || item.getQuantity() <= 0) {
            return;
        }
        droppedItems.add(new DroppedItem(worldX, worldY, item, vx, vy, pickupDelay));
    }

    public void spawnTileDrop(int tileX, int tileY, Item item) {
        if (item == null || item.getTileType() == null
                || item.getTileType() == TileType.AIR || item.getQuantity() <= 0) {
            return;
        }
        int tileSize = EnginePanel.tileSquare;
        double centerX = tileX * tileSize + tileSize / 2.0;
        double centerY = tileY * tileSize + tileSize / 2.0;
        double vx = (random.nextDouble() - 0.5) * 2.4;
        double vy = -2.2 - random.nextDouble() * 1.4;
        spawnDroppedItem(centerX, centerY, item, vx, vy, 12);
    }

    public void spawnContainerDrops(int tileX, int tileY, List<Item> containerItems) {
        if (containerItems == null || containerItems.isEmpty()) {
            return;
        }
        int tileSize = EnginePanel.tileSquare;
        double centerX = tileX * tileSize + tileSize / 2.0;
        double centerY = tileY * tileSize + tileSize / 2.0;

        if (containerItems.size() >= LOOT_BAG_CLUSTER_THRESHOLD) {
            DroppedItem bag = new DroppedItem(centerX, centerY, containerItems, 0, -2.6, 15);
            bag.getBundle().forceLootBagMode();
            droppedItems.add(bag);
        } else {
            for (Item item : containerItems) {
                spawnTileDrop(tileX, tileY, item);
            }
        }
    }

    public void update(Player player) {
        if (droppedItems.isEmpty()) {
            return;
        }

        List<DroppedItem> itemsToRemove = null;
        double maxWorldY = EnginePanel.CHUNK_HEIGHT * EnginePanel.tileSquare;

        for (DroppedItem drop : droppedItems) {
            boolean magnetized = updateMagnetism(drop, player);
            drop.updatePhysics(world, magnetized);

            if (drop.canBePickedUp() && player != null && !player.isDead()) {
                Rectangle pickupBox = drop.getBounds();
                pickupBox.grow(6, 6);
                if (pickupBox.intersects(player.getBounds())) {
                    collectIntoPlayer(drop, player);
                }
            }

            if (drop.isExpired() || drop.worldY > maxWorldY) {
                if (itemsToRemove == null) {
                    itemsToRemove = new ArrayList<>();
                }
                itemsToRemove.add(drop);
            }
        }

        if (itemsToRemove != null) {
            droppedItems.removeAll(itemsToRemove);
        }

        mergeTickCounter++;
        if (mergeTickCounter >= ITEM_MERGE_CHECK_INTERVAL && droppedItems.size() > 1) {
            mergeTickCounter = 0;
            mergeNearbyItems();
        }
    }

    private boolean updateMagnetism(DroppedItem drop, Player player) {
        if (!drop.canBePickedUp() || player == null || player.isDead()) {
            return false;
        }

        double targetX = player.getCenterX();
        double targetY = player.worldY + player.height * 0.6;
        double dx = targetX - drop.getCenterX();
        double dy = targetY - drop.getCenterY();
        double distSq = dx * dx + dy * dy;

        if (distSq <= MAGNET_RADIUS_PIXELS * MAGNET_RADIUS_PIXELS && canPlayerAcceptAny(player, drop.getBundle())) {
            drop.applyMagnetVelocity(targetX, targetY, Math.sqrt(distSq), MAGNET_RADIUS_PIXELS);
            return true;
        }
        return false;
    }

    private boolean canPlayerAcceptAny(Player player, DroppedItemBundle bundle) {
        for (Item item : bundle.getItems()) {
            if (player.canAcceptItem(item)) {
                return true;
            }
        }
        return false;
    }

    private void collectIntoPlayer(DroppedItem drop, Player player) {
        DroppedItemBundle bundle = drop.getBundle();
        boolean pickedUpAny = false;

        Iterator<Item> it = bundle.getItems().iterator();
        while (it.hasNext()) {
            Item item = it.next();
            int before = item.getQuantity();
            player.addItem(item);
            if (item.getQuantity() < before) {
                pickedUpAny = true;
            }
            if (item.getQuantity() <= 0) {
                it.remove();
            }
        }

        if (pickedUpAny) {
            drop.triggerPopAnimation();
            bundle.refreshModeAfterCollection();
        }
        if (bundle.isEmpty()) {
            drop.markForRemoval();
        }
    }

    private void mergeNearbyItems() {
        int n = droppedItems.size();
        double radiusSq = ITEM_MERGE_RADIUS_PIXELS * ITEM_MERGE_RADIUS_PIXELS;
        List<DroppedItem> mergedOut = null;

        for (int i = 0; i < n; i++) {
            DroppedItem a = droppedItems.get(i);
            if (a.isExpired()) {
                continue;
            }

            List<DroppedItem> cluster = null;
            for (int j = i + 1; j < n; j++) {
                DroppedItem b = droppedItems.get(j);
                if (b.isExpired()) {
                    continue;
                }

                double dx = a.getCenterX() - b.getCenterX();
                double dy = a.getCenterY() - b.getCenterY();
                if (dx * dx + dy * dy <= radiusSq) {
                    if (a.getBundle().canMergeAsSingleStackWith(b.getBundle())) {
                        a.absorbOther(b);
                        if (mergedOut == null) mergedOut = new ArrayList<>();
                        mergedOut.add(b);
                    } else {
                        if (cluster == null) cluster = new ArrayList<>();
                        cluster.add(b);
                    }
                }
            }

            if (cluster != null && (a.getBundle().isLootBag() || cluster.size() + 1 >= LOOT_BAG_CLUSTER_THRESHOLD)) {
                a.getBundle().forceLootBagMode();
                for (DroppedItem neighbor : cluster) {
                    if (!neighbor.isExpired()) {
                        a.absorbOther(neighbor);
                        if (mergedOut == null) mergedOut = new ArrayList<>();
                        mergedOut.add(neighbor);
                    }
                }
            }
        }

        if (mergedOut != null) {
            droppedItems.removeAll(mergedOut);
        }
    }

    public void draw(Graphics2D g2, Player player, WorldRenderer worldRenderer) {
        int tileSize = EnginePanel.tileSquare;

        for (DroppedItem drop : droppedItems) {
            int dropTileX = (int) Math.floor(drop.getCenterX() / tileSize);
            int dropTileY = (int) Math.floor(drop.getCenterY() / tileSize);
            int light = world.getLighting().getStaticLight(dropTileX, dropTileY);

            Item primary = drop.getBundle().getPrimaryItem();
            if (!drop.getBundle().isLootBag() && primary != null && primary.getTileType() == TileType.TORCH) {
                light = Math.max(light, 14);
            }

            Color shadowColor = worldRenderer.getShadowColor(light);
            renderer.draw(g2, drop, player, shadowColor);
        }
    }

    public int getDroppedItemCount() {
        return droppedItems.size();
    }
}