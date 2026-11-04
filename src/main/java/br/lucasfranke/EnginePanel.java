package br.lucasfranke;

import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.sound.SoundManager;
import br.lucasfranke.ui.UIManager;
import br.lucasfranke.world.WorldManager;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EnginePanel extends JPanel implements Runnable {

    private Thread thread;
    public static final int tileOriginalSquare = 16;
    public static final int scale = 3;
    public static final int tileSquare = tileOriginalSquare * scale;

    public static final int MIN_WORLD_HEIGHT = 0;
    public static final int MAX_WORLD_HEIGHT = 256;
    public static final int CHUNK_HEIGHT = 256;
    public static final int CHUNK_WIDTH = 16;

    public static final int maxScreenRow = 22;
    public static final int maxScreenCol = 40;

    public static final int WIDTH = tileSquare * maxScreenCol;
    public static final int HEIGHT = tileSquare * maxScreenRow;

    public static Player player;
    public static UIManager uiManager;
    public static WorldManager worldManager;
    public static SoundManager soundManager;

    private final KeyManager keyManager;
    private final MouseManager mouseManager;

    private final AtomicInteger renderedFramesCounter = new AtomicInteger(0);
    private volatile int currentFps = 0;
    private volatile int currentUps = 0;
    private volatile double lastRenderTimeMs = 0.0;

    public EnginePanel() {
        this.keyManager = new KeyManager();
        this.mouseManager = new MouseManager();

        addKeyListener(keyManager);
        addMouseListener(mouseManager);
        addMouseMotionListener(mouseManager);
        addMouseWheelListener(mouseManager);

        player = new Player();
        worldManager = new WorldManager();
        uiManager = new UIManager(player);
        soundManager = new SoundManager();

        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setFocusable(true);
    }

    public void startEngine() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        final int UPS = 60;
        final double timePerUpdate = 1000000000.0 / UPS;

        final int FPS = 60;
        final double timePerFrame = 1000000000.0 / FPS;

        long previousTime = System.nanoTime();
        double deltaU = 0;
        double deltaF = 0;

        int uFrames = 0;
        long timer = System.currentTimeMillis();

        player.linkManagers(worldManager);
        worldManager.spawnPlayerOnGround();

        while (thread != null && thread.isAlive()) {
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - previousTime;
            previousTime = currentTime;

            deltaU += elapsedTime / timePerUpdate;
            deltaF += elapsedTime / timePerFrame;

            boolean didWork = false;

            while (deltaU >= 1) {
                update();
                deltaU--;
                uFrames++;
                didWork = true;
            }

            if (deltaF >= 1) {
                repaint();
                deltaF--;
                didWork = true;
            }

            if (System.currentTimeMillis() - timer >= 1000) {
                currentFps = renderedFramesCounter.getAndSet(0);
                currentUps = uFrames;
                uFrames = 0;
                timer += 1000;
            }

            if (!didWork) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void update() {
        worldManager.update();
        player.update();
        uiManager.update();
    }

    @Override
    protected void paintComponent(Graphics g) {
        long renderStartNano = System.nanoTime();

        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        worldManager.draw(g2d);
        player.draw(g2d);
        uiManager.draw(g2d, WIDTH, HEIGHT);

        lastRenderTimeMs = (System.nanoTime() - renderStartNano) / 1_000_000.0;
        renderedFramesCounter.incrementAndGet();

        drawDebugHUD(g2d);
    }

    private void drawDebugHUD(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 120));
        g2d.fillRoundRect(6, 4, 330, 76, 8, 8);

        g2d.setFont(new Font("Arial", Font.BOLD, 15));
        g2d.setColor(Color.WHITE);
        g2d.drawString("X: " + (int) (player.worldX / tileSquare)
                + "  |  Y: " + (int) (player.worldY / tileSquare), 12, 22);
        g2d.drawString("Hour: " + worldManager.getFormattedTime()
                + " | Light: " + worldManager.world.getLighting().getSunLightLevel() + " [H: +1h]", 12, 44);

        if (currentFps >= 55) {
            g2d.setColor(new Color(100, 255, 120));
        } else if (currentFps >= 30) {
            g2d.setColor(new Color(255, 215, 80));
        } else {
            g2d.setColor(new Color(255, 85, 85));
        }

        String perfText = String.format("FPS: %d  |  UPS: %d  (Render: %.1f ms)",
                currentFps, currentUps, lastRenderTimeMs);
        g2d.drawString(perfText, 12, 66);
    }
}