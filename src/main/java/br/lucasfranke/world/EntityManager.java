package br.lucasfranke.world;

import br.lucasfranke.EnginePanel;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.entity.*;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.model.type.TileType;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class EntityManager {
    private static final int MAX_PASSIVE_MOBS = 4;
    private static final int MAX_HOSTILE_MOBS = 5;
    private static final int SPAWN_INTERVAL_TICKS = 240; // 4 sec per 60 UPS


    private static final int HALF_SCREEN_TILES = EnginePanel.maxScreenCol / 2;
    private static final int MIN_SPAWN_TILE_DIST = HALF_SCREEN_TILES + 5;  // 25 blocks
    private static final int MAX_SPAWN_TILE_DIST = HALF_SCREEN_TILES + 20; // 40 blocks
    private static final double DESPAWN_DISTANCE_PIXELS = EnginePanel.tileSquare * 64.0;
    private static final double MIN_MOB_SEPARATION_PIXELS = EnginePanel.tileSquare * 10.0;

    private final World world;
    private final Random random = new Random();
    private int spawnTimer = 0;

    private final List<Mob> mobs = new CopyOnWriteArrayList<>();

    public EntityManager(World world) {
        this.world = world;
    }

    public void update(Player player) {
        spawnTimer++;
        if (spawnTimer >= SPAWN_INTERVAL_TICKS) {
            spawnTimer = 0;
            attemptNaturalSpawn(player);
        }

        Rectangle playerBounds = player.getBounds();
        List<Mob> toRemove = new ArrayList<>();

        for (Mob mob : mobs) {
            mob.update(world, player);

            if (mob.getMobType().isHostile() && !mob.isDead() && mob.getBounds().intersects(playerBounds)) {
                player.takeDamage(mob.getMobType().getAttackDamage(), mob.getCenterX(), 6.5);
            }

            if (mob.isDead()) {
                TileType drop = mob.getMobType().getDropItem();
                int qty = mob.calculateDropAmount();
                if (drop != TileType.AIR && qty > 0) {
                    player.addItem(drop, qty);
                }
                toRemove.add(mob);
                continue;
            }

            double dist = Math.abs(mob.getCenterX() - player.getCenterX());
            if (dist > DESPAWN_DISTANCE_PIXELS || mob.worldY > EnginePanel.CHUNK_HEIGHT * EnginePanel.tileSquare) {
                toRemove.add(mob);
            }
        }

        if (!toRemove.isEmpty()) {
            mobs.removeAll(toRemove);
        }
    }

    public boolean handlePlayerMeleeAttack(Player player, int attackDamage, double reachPixels) {
        double mouseWorldX = player.worldX - player.playerX + MouseManager.getX();
        double mouseWorldY = player.worldY - player.playerY + MouseManager.getY();

        Mob closestTarget = null;
        double closestDistSq = reachPixels * reachPixels;

        for (Mob mob : mobs) {
            double dx = mob.getCenterX() - player.getCenterX();
            double dy = mob.getCenterY() - player.getCenterY();
            double distSq = dx * dx + dy * dy;

            if (distSq > closestDistSq) {
                continue;
            }

            Rectangle attackBox = mob.getBounds();
            attackBox.grow(18, 18);
            boolean cursorNearMob = attackBox.contains(mouseWorldX, mouseWorldY);
            boolean facingMob = (player.getDirection() == Direction.RIGHT && dx >= -16)
                    || (player.getDirection() == Direction.LEFT && dx <= 16);

            if (cursorNearMob || facingMob) {
                closestTarget = mob;
                closestDistSq = distSq;
            }
        }

        if (closestTarget != null) {
            return closestTarget.takeDamage(attackDamage, player.getCenterX(), 7.5);
        }
        return false;
    }

    public boolean intersectsAnyMob(Rectangle tileBounds) {
        for (Mob mob : mobs) {
            if (mob.getBounds().intersects(tileBounds)) {
                return true;
            }
        }
        return false;
    }

    private void attemptNaturalSpawn(Player player) {
        int passiveCount = 0;
        int hostileCount = 0;
        for (Mob m : mobs) {
            if (m.getMobType().isHostile()) {
                hostileCount++;
            } else {
                passiveCount++;
            }
        }

        if (passiveCount >= MAX_PASSIVE_MOBS && hostileCount >= MAX_HOSTILE_MOBS) {
            return;
        }

        int tileSize = EnginePanel.tileSquare;
        int playerTileX = (int) Math.floor(player.getCenterX() / tileSize);

        int offsetTiles = MIN_SPAWN_TILE_DIST + random.nextInt(MAX_SPAWN_TILE_DIST - MIN_SPAWN_TILE_DIST + 1);
        int spawnTileX = playerTileX + (random.nextBoolean() ? offsetTiles : -offsetTiles);

        double spawnX = spawnTileX * tileSize + 4;

        double screenX = spawnX - player.worldX + player.playerX;
        if (screenX >= -tileSize && screenX <= EnginePanel.WIDTH + tileSize) {
            return;
        }

        if (isTooCloseToExistingMob(spawnX)) {
            return;
        }

        world.getChunkByTileX(spawnTileX);

        int surfaceY = findSurfaceAirTileY(spawnTileX);
        if (surfaceY <= 2 || surfaceY >= EnginePanel.CHUNK_HEIGHT - 2) {
            return;
        }

        int lightLevel = world.getLighting().getStaticLight(spawnTileX, surfaceY);
        TileType groundBlock = world.getBlock(spawnTileX, surfaceY + 1);
        double spawnY = (surfaceY + 1) * tileSize;

        if (lightLevel <= 6 && hostileCount < MAX_HOSTILE_MOBS) {
            ZombieMob zombie = new ZombieMob(spawnX, spawnY - 46);
            if (!zombie.collidesAt(world, zombie.worldX, zombie.worldY)) {
                mobs.add(zombie);
            }
        } else if (lightLevel >= 9 && groundBlock == TileType.GRASS && passiveCount < MAX_PASSIVE_MOBS) {
            Mob passive = random.nextBoolean()
                    ? new CowMob(spawnX, spawnY - 36)
                    : new ChickenMob(spawnX, spawnY - 30);
            if (!passive.collidesAt(world, passive.worldX, passive.worldY)) {
                mobs.add(passive);
            }
        }
    }

    private boolean isTooCloseToExistingMob(double candidateWorldX) {
        for (Mob mob : mobs) {
            if (Math.abs(mob.worldX - candidateWorldX) < MIN_MOB_SEPARATION_PIXELS) {
                return true;
            }
        }
        return false;
    }

    private int findSurfaceAirTileY(int tileX) {
        for (int y = 1; y < EnginePanel.CHUNK_HEIGHT - 1; y++) {
            if (!world.isSolid(tileX, y) && world.isSolid(tileX, y + 1)) {
                return y;
            }
        }
        return -1;
    }

    public void draw(Graphics2D g2, Player player, WorldRenderer renderer) {
        int tileSize = EnginePanel.tileSquare;
        for (Mob mob : mobs) {
            int mobTileX = (int) Math.floor(mob.getCenterX() / tileSize);
            int mobTileY = (int) Math.floor(mob.getCenterY() / tileSize);
            int light = world.getLighting().getStaticLight(mobTileX, mobTileY);
            Color shadowColor = renderer.getShadowColor(light);
            mob.draw(g2, player, shadowColor);
        }
    }

    public int getActiveMobCount() {
        return mobs.size();
    }
}