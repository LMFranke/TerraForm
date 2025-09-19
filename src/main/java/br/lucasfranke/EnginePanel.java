package br.lucasfranke;

import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.listeners.MouseManager;
import br.lucasfranke.model.entity.Player;
import br.lucasfranke.sound.SoundManager;
import br.lucasfranke.ui.UIManager;
import br.lucasfranke.world.WorldManager;

import javax.swing.*;
import java.awt.*;

public class EnginePanel extends JPanel implements Runnable {

    private Thread thread;

    public static final int tileOriginalSquare = 16;
    public static final int scale = 3;
    public static final int tileSquare = tileOriginalSquare * scale;

    public static final int maxScreenRow = 20;
    public static final int maxScreenCol = 30;

    public static final int chunkSize = 16;

    public static final int WIDTH = tileSquare * maxScreenCol;
    public static final int HEIGHT = tileSquare * maxScreenRow;

    public static Player player;
    public static UIManager uiManager;
    public static WorldManager worldManager;
    public static SoundManager soundManager;

    private final KeyManager keyManager;
    private final MouseManager mouseManager;

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

        final int FPS = 144;
        final double timePerFrame = 1000000000.0 / FPS;

        long previousTime = System.nanoTime();
        double deltaU = 0;
        double deltaF = 0;

        int frames = 0;
        double time = System.currentTimeMillis();

        player.linkManagers(worldManager);

        while (thread != null && thread.isAlive()) {
            long currentTime = System.nanoTime();
            long elapsedTime = currentTime - previousTime;
            previousTime = currentTime;

            deltaU += elapsedTime / timePerUpdate;
            deltaF += elapsedTime / timePerFrame;

            while (deltaU >= 1) {
                update();
                deltaU--;
            }

            if (deltaF >= 1) {
                repaint();
                deltaF--;
                frames++;
            }

            if(System.currentTimeMillis() - time >= 1000) {
                System.out.println("fps:" + frames);
                time += 1000;
                frames = 0;
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
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        worldManager.draw(g2d);
        player.draw(g2d);
        uiManager.draw(g2d, WIDTH, HEIGHT);

        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        g2d.drawString("X: " + (int) (player.worldX / tileSquare), 10, 20);
        g2d.drawString("Y: " + (int) (player.worldY / tileSquare), 10, 35);
    }

}
