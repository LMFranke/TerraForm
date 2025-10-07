package br.lucasfranke.model.entity;

import br.lucasfranke.Assets;
import br.lucasfranke.EnginePanel;
import br.lucasfranke.Sprite;
import br.lucasfranke.inventory.HotBar;
import br.lucasfranke.inventory.Inventory;
import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.type.AnimationState;
import br.lucasfranke.model.type.Direction;
import br.lucasfranke.model.type.TileType;
import br.lucasfranke.world.Chunk;
import br.lucasfranke.world.World;
import br.lucasfranke.world.WorldManager;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class Player extends Entity {

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

    /**
     * Process all the player's input
     */
    private void handleInput() {
        if (KeyManager.isKeyPressed(KeyEvent.VK_E)) {
            EnginePanel.uiManager.toggleInventory(inventory);
            KeyManager.unpressKey(KeyEvent.VK_E);
        }

        if (MouseManager.isButtonPressed(MouseEvent.BUTTON1) && !EnginePanel.uiManager.isInventoryOpen()) {
            if (worldManager.hoveredTileWorldPosition[0] != -1) {
                worldManager.handleBlockClick();
            }
            MouseManager.unpressButton(MouseEvent.BUTTON1);
        }

        // process the movent inputs
        double moveX = 0;
        double moveY = 0;

        if (KeyManager.isKeyPressed(KeyEvent.VK_A)) moveX -= 1;
        if (KeyManager.isKeyPressed(KeyEvent.VK_D)) moveX += 1;

        if (moveX != 0) {
            direction = moveX > 0 ? Direction.RIGHT : Direction.LEFT;
        }

        if (moveX != 0) {
            moveX *= speed;
        }

        if (KeyManager.isKeyPressed(KeyEvent.VK_SPACE) && !isFalling) {
            fallSpeed = -6;
            isFalling = true;
        }

        fallSpeed += gravity;
        if (fallSpeed > maxFallSpeed) fallSpeed = maxFallSpeed;
        moveY += fallSpeed;

        move(moveX, moveY);
    }

    /**
     * Moves the entity by dx and dy, checking collision on each axis separately to allow the player to slide on walls
     */
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

    /**
     * Update the animation based on the current player's state
     */
    private void updateAnimation() {
        boolean isMovingHorizontally = KeyManager.isKeyPressed(KeyEvent.VK_A) || KeyManager.isKeyPressed(KeyEvent.VK_D);

        state = isMovingHorizontally ? AnimationState.WALK : AnimationState.IDLE;

        if (state == AnimationState.IDLE) {
            if (KeyManager.isKeyPressed(KeyEvent.VK_W)) direction = Direction.UP;
            if (KeyManager.isKeyPressed(KeyEvent.VK_S)) direction = Direction.DOWN;
        }

        setAnimation();
    }

    /**
     * Define the correct animation on sprite, avoiding unnecessary reset
     */
    private void setAnimation() {
        String key = state.name() + "_" + direction.name();
        Sprite newAnimation = animations.get(key);
        if (sprite != newAnimation) {
            sprite = newAnimation;
            sprite.reset();
        }
    }

    /**
     * Verify if the entity will collide on a solid block in a future position
     */
    private boolean collidesAt(double testX, double testY) {
        int tileSize = EnginePanel.tileSquare;

        double leftTile = testX / tileSize;
        double rightTile = (testX + width - 1) / tileSize;
        double topTile = testY / tileSize;
        double bottomTile = (testY + height - 1) / tileSize;

        for (int ty = (int) topTile; ty <= bottomTile; ty++) {
            for (int tx = (int) leftTile; tx <= rightTile; tx++) {
                int cx = Math.floorDiv(tx, EnginePanel.CHUNK_WIDTH);

                int col = Math.floorMod(tx, EnginePanel.CHUNK_WIDTH);
                int row = Math.floorMod(ty, EnginePanel.CHUNK_HEIGHT);

                Chunk chunk = world.getChunk(cx);
                if (chunk == null) {
                    continue;
                }

                TileType tile = chunk.blocks[row][col];
                if (tile != null && tile.isSolid()) {
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
