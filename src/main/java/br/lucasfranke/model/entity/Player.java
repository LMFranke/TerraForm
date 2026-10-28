package br.lucasfranke.model.entity;

import br.lucasfranke.Assets;
import br.lucasfranke.EnginePanel;
import br.lucasfranke.Sprite;
import br.lucasfranke.inventory.HotBar;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.inventory.ItemContainer;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.block.CraftingTableBlock;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.AnimationState;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;
import br.lucasfranke.world.World;
import br.lucasfranke.world.WorldManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class Player extends Entity {
    public static final int MAX_STACK_SIZE = 64;
    private static final double COMBAT_REACH_PIXELS = EnginePanel.tileSquare * 2.4;

    private WorldManager worldManager;
    private World world;
    private final Map<String, Sprite> animations = new HashMap<>();

    public final int playerX;
    public final int playerY;
    public Inventory inventory;
    public HotBar hotBar;
    public final CraftingTableBlock personalCrafting;

    private int swingTick = 0;
    private int attackCooldown = 0;

    public Player() {
        super();
        this.worldX = EnginePanel.tileSquare * 10000.0;
        this.worldY = EnginePanel.tileSquare * 0;
        this.playerX = EnginePanel.WIDTH / 2 - (EnginePanel.tileSquare / 2);
        this.playerY = EnginePanel.HEIGHT / 2 - (EnginePanel.tileSquare / 2);
        this.inventory = new Inventory(5, 9);
        this.hotBar = new HotBar();
        this.personalCrafting = new CraftingTableBlock(0, 0, true);
        this.width = EnginePanel.tileSquare - 8;
        this.height = EnginePanel.tileSquare - 2;
        this.speed = 5;
        this.gravity = 0.3;
        this.maxFallSpeed = 12;
        this.maxHealth = 20;
        this.health = 20;
        this.direction = Direction.RIGHT;
        this.state = AnimationState.IDLE;
        initSprite();
        setAnimation();
    }

    public void linkManagers(WorldManager worldManager) {
        this.worldManager = worldManager;
        this.world = worldManager.world;
    }

    @Override
    public void update() {
        tickTimers();
        if (attackCooldown > 0) {
            attackCooldown--;
        }
        if (isDead()) {
            respawn();
            return;
        }
        handleInput();
        inventory.update();
        hotBar.update();
        updateAnimation();
        sprite.update();
    }

    private void respawn() {
        this.health = this.maxHealth;
        this.hurtTimer = 40;
        this.fallSpeed = 0;
        this.knockbackVx = 0;
        worldManager.spawnPlayerOnGround();
    }

    @Override
    public void draw(Graphics2D g) {
        g.drawImage(sprite.getFrame(), playerX, playerY, EnginePanel.tileSquare, EnginePanel.tileSquare, null);
        if (hurtTimer > 0) {
            g.setColor(new Color(255, 40, 40, 115));
            g.fillRoundRect(playerX + 4, playerY + 2, width, height, 6, 6);
        }
        drawHeldTool(g);
    }

    private void drawHeldTool(Graphics2D g) {
        Item held = hotBar.getCurrentItem();
        if (held == null || (!held.isTool() && !held.getTileType().isFood()) || held.getTileType().getSprite() == null) {
            return;
        }
        BufferedImage toolSprite = held.getTileType().getSprite();
        int toolSize = (int) (EnginePanel.tileSquare * 0.65);
        boolean facingLeft = (direction == Direction.LEFT);
        Graphics2D g2d = (Graphics2D) g.create();
        int handX = facingLeft ? playerX + 6 : playerX + EnginePanel.tileSquare - 10;
        int handY = playerY + EnginePanel.tileSquare / 2 + 4;
        double angle = 0;
        if (swingTick > 0) {
            double swingPhase = Math.sin(swingTick * 0.45);
            angle = facingLeft ? -swingPhase * 0.85 : swingPhase * 0.85;
        }
        AffineTransform oldTransform = g2d.getTransform();
        g2d.translate(handX, handY);
        g2d.rotate(angle);
        if (facingLeft) {
            g2d.drawImage(toolSprite, 4, -toolSize + 6, -toolSize, toolSize, null);
        } else {
            g2d.drawImage(toolSprite, -4, -toolSize + 6, toolSize, toolSize, null);
        }
        g2d.setTransform(oldTransform);
        g2d.dispose();
    }

    public boolean isBackgroundMode() {
        return KeyManager.isKeyPressed(KeyEvent.VK_SHIFT);
    }

    private void handleInput() {
        if (KeyManager.isKeyPressed(KeyEvent.VK_E)) {
            EnginePanel.uiManager.toggleInventory(inventory);
            KeyManager.unpressKey(KeyEvent.VK_E);
        }
        if (!EnginePanel.uiManager.isInventoryOpen()) {
            if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
                swingTick++;
                boolean hitMob = tryCombatAttack();
                if (!hitMob && worldManager.hoveredTileWorldPosition[0] != -1) {
                    worldManager.handleBlockInteraction();
                } else if (hitMob) {
                    worldManager.resetMining();
                }
            } else {
                swingTick = 0;
                worldManager.resetMining();
            }
            if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
                if (!tryConsumeHeldFood() && worldManager.hoveredTileWorldPosition[0] != -1) {
                    worldManager.handleBlockInteraction();
                }
                MouseManager.unpressButton(MouseEvent.BUTTON3);
            }
        } else {
            swingTick = 0;
            worldManager.resetMining();
        }

        double moveX = 0;
        if (KeyManager.isKeyPressed(KeyEvent.VK_A)) moveX -= 1;
        if (KeyManager.isKeyPressed(KeyEvent.VK_D)) moveX += 1;
        if (moveX != 0) {
            direction = moveX > 0 ? Direction.RIGHT : Direction.LEFT;
            moveX *= speed;
        }
        if (KeyManager.isKeyPressed(KeyEvent.VK_SPACE) && !isFalling) {
            fallSpeed = -6;
            isFalling = true;
            EnginePanel.soundManager.playSound(Sound.PLAYER_JUMP);
        }
        fallSpeed += gravity;
        if (fallSpeed > maxFallSpeed) fallSpeed = maxFallSpeed;

        moveWithCollision(world, moveX + knockbackVx, fallSpeed);
    }

    private boolean tryCombatAttack() {
        if (attackCooldown > 0) {
            return false;
        }
        Item held = hotBar.getCurrentItem();
        int damage = (held != null && held.getTileType() != null)
                ? held.getTileType().getAttackDamage()
                : 2;

        boolean hit = worldManager.entityManager.handlePlayerMeleeAttack(this, damage, COMBAT_REACH_PIXELS);
        if (hit) {
            attackCooldown = 16;
            if (held != null && held.isTool()) {
                if (held.damage(1)) {
                    hotBar.setItem(0, hotBar.getSelectedSlot(), null);
                    hotBar.setCurrentItem(null);
                }
            }
        }
        return hit;
    }

    private boolean tryConsumeHeldFood() {
        Item held = hotBar.getCurrentItem();
        if (held == null || held.getQuantity() <= 0 || !held.getTileType().isFood()) {
            return false;
        }
        if (health >= maxHealth) {
            return false;
        }
        heal(held.getTileType().getFoodHealAmount());
        held.decreaseQuantity();
        if (held.getQuantity() <= 0) {
            hotBar.setItem(0, hotBar.getSelectedSlot(), null);
            hotBar.setCurrentItem(null);
        }
        EnginePanel.soundManager.playSound(Sound.ITEM_PICKUP);
        return true;
    }

    public boolean addItem(Item item) {
        if (item == null || item.getTileType() == null || item.getTileType() == TileType.AIR || item.getQuantity() <= 0) {
            return false;
        }
        if (item.isTool()) {
            if (placeToolItemInEmptySlot(hotBar, item) || placeToolItemInEmptySlot(inventory, item)) {
                EnginePanel.soundManager.playSound(Sound.ITEM_PICKUP);
                return true;
            }
            return false;
        }
        return addItem(item.getTileType(), item.getQuantity());
    }

    public boolean addItem(TileType type, int amount) {
        if (type == null || type == TileType.AIR || amount <= 0) {
            return false;
        }
        int remaining = amount;
        if (!type.isTool()) {
            remaining = stackIntoContainer(hotBar, type, remaining);
            if (remaining > 0) {
                remaining = stackIntoContainer(inventory, type, remaining);
            }
        }
        if (remaining > 0) {
            remaining = placeInEmptySlot(hotBar, type, remaining);
        }
        if (remaining > 0) {
            remaining = placeInEmptySlot(inventory, type, remaining);
        }
        if (remaining < amount) {
            EnginePanel.soundManager.playSound(Sound.ITEM_PICKUP);
            return true;
        }
        return false;
    }

    private boolean placeToolItemInEmptySlot(ItemContainer container, Item toolItem) {
        for (int r = 0; r < container.getRows(); r++) {
            for (int c = 0; c < container.getCols(); c++) {
                if (container.getItem(r, c) == null) {
                    container.setItem(r, c, toolItem.copyWithQuantity(1));
                    return true;
                }
            }
        }
        return false;
    }

    private int stackIntoContainer(ItemContainer container, TileType type, int remaining) {
        if (type.isTool()) {
            return remaining;
        }
        for (int r = 0; r < container.getRows(); r++) {
            for (int c = 0; c < container.getCols(); c++) {
                Item slotItem = container.getItem(r, c);
                if (slotItem != null && slotItem.getTileType() == type && slotItem.getQuantity() < slotItem.getMaxStackSize()) {
                    int space = slotItem.getMaxStackSize() - slotItem.getQuantity();
                    int toAdd = Math.min(space, remaining);
                    slotItem.increaseQuantity(toAdd);
                    remaining -= toAdd;
                    if (remaining <= 0) {
                        return 0;
                    }
                }
            }
        }
        return remaining;
    }

    private int placeInEmptySlot(ItemContainer container, TileType type, int remaining) {
        int maxStack = type.isTool() ? 1 : MAX_STACK_SIZE;
        for (int r = 0; r < container.getRows(); r++) {
            for (int c = 0; c < container.getCols(); c++) {
                if (container.getItem(r, c) == null) {
                    int toPlace = Math.min(maxStack, remaining);
                    container.setItem(r, c, new Item(type, toPlace));
                    remaining -= toPlace;
                    if (remaining <= 0) {
                        return 0;
                    }
                }
            }
        }
        return remaining;
    }

    private void updateAnimation() {
        boolean isMovingHorizontally = KeyManager.isKeyPressed(KeyEvent.VK_A) || KeyManager.isKeyPressed(KeyEvent.VK_D);
        state = isMovingHorizontally ? AnimationState.WALK : AnimationState.IDLE;
        if (state == AnimationState.IDLE) {
            if (KeyManager.isKeyPressed(KeyEvent.VK_W) || KeyManager.isKeyPressed(KeyEvent.VK_SHIFT)) {
                direction = Direction.UP;
            }
            if (KeyManager.isKeyPressed(KeyEvent.VK_S)) {
                direction = Direction.DOWN;
            }
        }
        setAnimation();
    }

    private void setAnimation() {
        String key = state.name() + "_" + direction.name();
        Sprite newAnimation = animations.get(key);
        if (sprite != newAnimation) {
            sprite = newAnimation;
            sprite.reset();
        }
    }

    private void initSprite() {
        animations.put("WALK_RIGHT", new Sprite(Assets.playerWalkRight, 12));
        animations.put("WALK_LEFT", new Sprite(Assets.playerWalkLeft, 12));
        animations.put("WALK_UP", new Sprite(Assets.playerWalkUp, 12));
        animations.put("WALK_DOWN", new Sprite(Assets.playerWalkDown, 12));
        animations.put("IDLE_RIGHT", new Sprite(Assets.playerIdleRight, 20));
        animations.put("IDLE_LEFT", new Sprite(Assets.playerIdleLeft, 20));
        animations.put("IDLE_UP", new Sprite(Assets.playerIdleUp, 20));
        animations.put("IDLE_DOWN", new Sprite(Assets.playerIdleDown, 20));
    }
}