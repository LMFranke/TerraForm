package br.lucasfranke;

import br.lucasfranke.model.type.TileType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Assets {

    public static BufferedImage[] playerWalkRight;
    public static BufferedImage[] playerWalkLeft;
    public static BufferedImage[] playerWalkUp;
    public static BufferedImage[] playerWalkDown;
    public static BufferedImage[] playerIdleRight;
    public static BufferedImage[] playerIdleLeft;
    public static BufferedImage[] playerIdleUp;
    public static BufferedImage[] playerIdleDown;

    public static void init() throws Exception {
        TileType.GRASS.setSprite(load("src/main/resources/image/tiles/grassTile.png"));
        TileType.DIRT.setSprite(load("src/main/resources/image/tiles/dirtTile.png"));
        TileType.STONE.setSprite(load("src/main/resources/image/tiles/stoneTile.png"));
        TileType.AIR.setSprite(load("src/main/resources/image/tiles/skyTile.png"));
        TileType.BACKGROUND_DIRT.setSprite(load("src/main/resources/image/tiles/backgroundDirtTile.png"));
        TileType.BACKGROUND_GRASS.setSprite(load("src/main/resources/image/tiles/backgroundGrassTile.png"));
        TileType.BACKGROUND_STONE.setSprite(load("src/main/resources/image/tiles/backgroundStoneTile.png"));

        TileType.TORCH.setAnimation(
                loadSpriteSheet("src/main/resources/image/tiles/torchTile.png", EnginePanel.tileOriginalSquare, EnginePanel.tileOriginalSquare),
                10
        );

        playerWalkRight = new BufferedImage[] { load("src/main/resources/image/player/boy_right_1.png"), load("src/main/resources/image/player/boy_right_2.png")};
        playerWalkLeft  = new BufferedImage[] { load("src/main/resources/image/player/boy_left_1.png"), load("src/main/resources/image/player/boy_left_2.png")};
        playerWalkUp = new BufferedImage[] { load("src/main/resources/image/player/boy_up_1.png"), load("src/main/resources/image/player/boy_up_2.png")};
        playerWalkDown  = new BufferedImage[] { load("src/main/resources/image/player/boy_down_1.png"), load("src/main/resources/image/player/boy_down_2.png")};

        playerIdleDown = new BufferedImage[] {load("src/main/resources/image/player/boy_down_1_idle.png")};
        playerIdleLeft = new BufferedImage[] {load("src/main/resources/image/player/boy_left_1_idle.png")};
        playerIdleUp = new BufferedImage[] {load("src/main/resources/image/player/boy_up_1_idle.png")};
        playerIdleRight = new BufferedImage[] {load("src/main/resources/image/player/boy_right_1_idle.png")};
    }

    private static BufferedImage load(String filename) throws IOException {
        return ImageIO.read(new File(filename));
    }

    private static BufferedImage[] loadSpriteSheet(String filename, int frameWidth, int frameHeight) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                BufferedImage sheet = ImageIO.read(file);
                int cols = Math.max(1, sheet.getWidth() / frameWidth);
                int rows = Math.max(1, sheet.getHeight() / frameHeight);
                BufferedImage[] frames = new BufferedImage[cols * rows];

                int index = 0;
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        frames[index++] = sheet.getSubimage(
                                c * frameWidth,
                                r * frameHeight,
                                frameWidth,
                                frameHeight
                        );
                    }
                }
                return frames;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        BufferedImage fallback = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fallback.createGraphics();
        g.setColor(new Color(120, 72, 32));
        g.fillRect(7, 7, 2, 7);
        g.setColor(new Color(255, 140, 0));
        g.fillRect(6, 4, 4, 3);
        g.dispose();
        return new BufferedImage[]{fallback};
    }
}