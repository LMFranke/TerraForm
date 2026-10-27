package br.lucasfranke.model.entity;

import br.lucasfranke.Assets;
import br.lucasfranke.EnginePanel;
import br.lucasfranke.Sprite;
import br.lucasfranke.inventory.HotBar;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.inventory.ItemContainer;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.item.Item;
import br.lucasfranke.model.type.AnimationState;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.sound.Sound;
import br.lucasfranke.world.Chunk;
import br.lucasfranke.world.World;
import br.lucasfranke.world.WorldManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class Player extends Entity {

    public static final int MAX_STACK_SIZE = 64;

    private WorldManager worldManager;
    private World world;

    private final Map<String, Sprite> animations = new HashMap<>();

    public double worldX;
    public double worldY;

    public final int playerX;
    public final int playerY;

    public Inventory inventory;
    public HotBar hotBar;

    public Player() {
        super();

        this.worldX = EnginePanel.tileSquare * 10000.0;
        this.worldY = EnginePanel.tileSquare * 0;

        this.playerX = EnginePanel.WIDTH / 2 - (EnginePanel.tileSquare / 2);
        this.playerY = EnginePanel.HEIGHT / 2 - (EnginePanel.tileSquare / 2);

        this.inventory = new Inventory(5, 9);
        this.hotBar = new HotBar();

        this.width = EnginePanel.tileSquare - 8;
        this.height = EnginePanel.tileSquare - 2;

        this.speed = 5;
        this.gravity = 0.3;
        this.maxFallSpeed = 12;

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
        handleInput();
        inventory.update();
        hotBar.update();
        updateAnimation();
        sprite.update();
    }

    @Override
    public void draw(Graphics2D g) {
        g.drawImage(sprite.getFrame(), playerX, playerY, EnginePanel.tileSquare, EnginePanel.tileSquare, null);
    }

    public boolean isBackgroundMode() {
        return KeyManager.isKeyPressed(KeyEvent.VK_SHIFT) || this.direction == Direction.UP;
    }

    private void handleInput() {
        if (KeyManager.isKeyPressed(KeyEvent.VK_E)) {
            EnginePanel.uiManager.toggleInventory(inventory);
            KeyManager.unpressKey(KeyEvent.VK_E);
        }

        if (!EnginePanel.uiManager.isInventoryOpen()) {
            if (MouseManager.isButtonPressed(MouseEvent.BUTTON1)) {
                if (worldManager.hoveredTileWorldPosition[0] != -1) {
                    worldManager.handleBlockInteraction();
                } else {
                    worldManager.resetMining();
                }
            } else {
                worldManager.resetMining();
            }

            if (MouseManager.isButtonPressed(MouseEvent.BUTTON3)) {
                if (worldManager.hoveredTileWorldPosition[0] != -1) {
                    worldManager.handleBlockInteraction();
                }
                MouseManager.unpressButton(MouseEvent.BUTTON3);
            }
        } else {
            worldManager.resetMining();
        }

        double moveX = 0;
        double moveY = 0;

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

        moveY += fallSpeed;

        move(moveX, moveY);
    }

    public boolean addItem(TileType type, int amount) {
        if (type == null || type == TileType.AIR || amount <= 0) {
            return false;
        }

        int remaining = amount;

        remaining = stackIntoContainer(hotBar, type, remaining);
        if (remaining > 0) {
            remaining = stackIntoContainer(inventory, type, remaining);
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

    private int stackIntoContainer(ItemContainer container, TileType type, int remaining) {
        for (int r = 0; r < container.getRows(); r++) {
            for (int c = 0; c < container.getCols(); c++) {
                Item slotItem = container.getItem(r, c);
                if (slotItem != null && slotItem.getTileType() == type && slotItem.getQuantity() < MAX_STACK_SIZE) {
                    int space = MAX_STACK_SIZE - slotItem.getQuantity();
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
        for (int r = 0; r < container.getRows(); r++) {
            for (int c = 0; c < container.getCols(); c++) {
                if (container.getItem(r, c) == null) {
                    int toPlace = Math.min(MAX_STACK_SIZE, remaining);
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

    private void move(double dx, double dy) {
        if (dx != 0 && !collidesAt(worldX + dx, worldY)) {
            worldX += dx;
        }

        isFalling = true;

        if (dy != 0 && !collidesAt(worldX, worldY + dy)) {
            worldY += dy;
        } else {
            if (dy > 0) {
                isFalling = false;
                worldY = (int) ((worldY + height + dy) / EnginePanel.tileSquare) * EnginePanel.tileSquare - height;
                fallSpeed = 0;
            } else if (dy < 0) {
                fallSpeed = 0;
            }
        }
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

    private boolean collidesAt(double testX, double testY) {
        int tileSize = EnginePanel.tileSquare;

        int leftTile = (int) Math.floor(testX / tileSize);
        int rightTile = (int) Math.floor((testX + width - 1) / tileSize);
        int topTile = (int) Math.floor(testY / tileSize);
        int bottomTile = (int) Math.floor((testY + height - 1) / tileSize);

        for (int ty = topTile; ty <= bottomTile; ty++) {
            for (int tx = leftTile; tx <= rightTile; tx++) {
                if (world.isSolid(tx, ty)) {
                    return true;
                }
            }
        }
        return false;
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