package br.lucasfranke;

import br.lucasfranke.model.type.TileType;
import br.lucasfranke.model.type.ToolCategory;
import br.lucasfranke.model.type.ToolTier;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Supplier;

public class Assets {

    public static BufferedImage genericTileSprite;

    public static BufferedImage[] playerWalkRight;
    public static BufferedImage[] playerWalkLeft;
    public static BufferedImage[] playerWalkUp;
    public static BufferedImage[] playerWalkDown;
    public static BufferedImage[] playerIdleRight;
    public static BufferedImage[] playerIdleLeft;
    public static BufferedImage[] playerIdleUp;
    public static BufferedImage[] playerIdleDown;

    public static BufferedImage[] zombieWalkLeft;
    public static BufferedImage[] zombieWalkRight;
    public static BufferedImage[] cowWalkLeft;
    public static BufferedImage[] cowWalkRight;
    public static BufferedImage[] chickenWalkLeft;
    public static BufferedImage[] chickenWalkRight;

    public static BufferedImage furnaceOffSprite;
    public static BufferedImage[] furnaceOnFrames;

    public static void init() {
        int size = EnginePanel.tileOriginalSquare;
        genericTileSprite = createGenericTexture(size, size);

        BufferedImage stoneSprite = load("src/main/resources/image/tiles/stoneTile.png");

        TileType.GRASS.setSprite(load("src/main/resources/image/tiles/grassTile.png"));
        TileType.DIRT.setSprite(load("src/main/resources/image/tiles/dirtTile.png"));
        TileType.STONE.setSprite(stoneSprite);
        TileType.AIR.setSprite(load("src/main/resources/image/tiles/skyTile.png"));

        TileType.BACKGROUND_DIRT.setSprite(load("src/main/resources/image/tiles/backgroundDirtTile.png"));
        TileType.BACKGROUND_GRASS.setSprite(load("src/main/resources/image/tiles/backgroundGrassTile.png"));
        TileType.BACKGROUND_STONE.setSprite(load("src/main/resources/image/tiles/backgroundStoneTile.png"));

        BufferedImage[] leavesSprite = loadSpriteSheet("src/main/resources/image/tiles/oakLeaveTile.png", 16, 16);
        BufferedImage woodSprite = loadOrGenerate("src/main/resources/image/tiles/oakLogTile.png", () -> createLogTexture(size, size));

        TileType.OAK_LEAVES.setAnimation(leavesSprite, 45);
        TileType.OAK_WOOD.setSprite(woodSprite);
        TileType.BACKGROUND_WOOD.setSprite(createDarkenedTexture(woodSprite, 0.55f));
        TileType.BACKGROUND_LEAVES.setAnimation(leavesSprite, 45);

        TileType.OAK_SAPLING.setSprite(load("src/main/resources/image/tiles/oakSapling.png"));

        TileType.PLANKS.setSprite(loadOrGenerate("src/main/resources/image/tiles/oakPlankTile.png", () -> createPlanksTexture(size, size, false)));
        TileType.BACKGROUND_PLANKS.setSprite(loadOrGenerate("src/main/resources/image/tiles/backgroundPlanksTile.png", () -> createPlanksTexture(size, size, true)));

        // Ores
        TileType.COAL_ORE.setSprite(loadOrGenerate("src/main/resources/image/tiles/coalOreTile.png", () -> createOreTexture(stoneSprite, new Color(25, 25, 30), new Color(55, 55, 65))));
        TileType.IRON_ORE.setSprite(loadOrGenerate("src/main/resources/image/tiles/ironOreTile.png", () -> createOreTexture(stoneSprite, new Color(210, 175, 145), new Color(240, 210, 185))));
        TileType.GOLD_ORE.setSprite(loadOrGenerate("src/main/resources/image/tiles/goldOreTile.png", () -> createOreTexture(stoneSprite, new Color(245, 200, 35), new Color(255, 240, 120))));

        // Material and refined items
        TileType.STICK.setSprite(loadOrGenerate("src/main/resources/image/tiles/stick.png", () -> createStickTexture(size, size)));
        TileType.COAL.setSprite(loadOrGenerate("src/main/resources/image/tiles/coal.png", () -> createCoalItemTexture(size, size)));
        TileType.IRON_INGOT.setSprite(loadOrGenerate("src/main/resources/image/tiles/ironIngot.png", () -> createIngotTexture(size, size, new Color(200, 205, 215), new Color(240, 245, 255))));
        TileType.GOLD_INGOT.setSprite(loadOrGenerate("src/main/resources/image/tiles/goldIngot.png", () -> createIngotTexture(size, size, new Color(235, 185, 30), new Color(255, 235, 110))));

        // Food & Mob drops
        TileType.RAW_BEEF.setSprite(loadOrGenerate("src/main/resources/image/tiles/rawBeef.png", () -> createMeatTexture(size, size, new Color(195, 55, 55), new Color(240, 185, 175))));
        TileType.COOKED_BEEF.setSprite(loadOrGenerate("src/main/resources/image/tiles/cookedBeef.png", () -> createMeatTexture(size, size, new Color(115, 62, 32), new Color(165, 105, 60))));
        TileType.RAW_CHICKEN.setSprite(loadOrGenerate("src/main/resources/image/tiles/rawChicken.png", () -> createMeatTexture(size, size, new Color(235, 165, 150), new Color(255, 225, 215))));
        TileType.COOKED_CHICKEN.setSprite(loadOrGenerate("src/main/resources/image/tiles/cookedChicken.png", () -> createMeatTexture(size, size, new Color(195, 125, 45), new Color(235, 175, 90))));
        TileType.ROTTEN_FLESH.setSprite(loadOrGenerate("src/main/resources/image/tiles/rottenFlesh.png", () -> createMeatTexture(size, size, new Color(125, 75, 55), new Color(95, 145, 75))));

        // Tools (Pickaxes, Axes, Shovels, Swords)
        for (TileType type : TileType.values()) {
            if (type.isTool()) {
                String toolFile = "src/main/resources/image/tiles/" + type.name().toLowerCase() + ".png";
                type.setSprite(loadOrGenerate(toolFile, () -> createToolTexture(size, size, type.getToolCategory(), type.getToolTier())));
            }
        }

        TileType.TORCH.setAnimation(loadSpriteSheet("src/main/resources/image/tiles/torchTile.png", size, size), 10);
        TileType.CHEST.setSprite(loadOrGenerate("src/main/resources/image/tiles/chestTile.png", () -> createChestTexture(size, size)));
        TileType.CRAFTING_TABLE.setSprite(loadOrGenerate("src/main/resources/image/tiles/craftingTableTile.png", () -> createCraftingTableTexture(size, size)));

        initFurnaceSprites(size);

        playerWalkRight = new BufferedImage[]{load("src/main/resources/image/player/boy_right_1.png"), load("src/main/resources/image/player/boy_right_2.png")};
        playerWalkLeft = new BufferedImage[]{load("src/main/resources/image/player/boy_left_1.png"), load("src/main/resources/image/player/boy_left_2.png")};
        playerWalkUp = new BufferedImage[]{load("src/main/resources/image/player/boy_up_1.png"), load("src/main/resources/image/player/boy_up_2.png")};
        playerWalkDown = new BufferedImage[]{load("src/main/resources/image/player/boy_down_1.png"), load("src/main/resources/image/player/boy_down_2.png")};

        playerIdleDown = new BufferedImage[]{load("src/main/resources/image/player/boy_down_1_idle.png")};
        playerIdleLeft = new BufferedImage[]{load("src/main/resources/image/player/boy_left_1_idle.png")};
        playerIdleUp = new BufferedImage[]{load("src/main/resources/image/player/boy_up_1_idle.png")};
        playerIdleRight = new BufferedImage[]{load("src/main/resources/image/player/boy_right_1_idle.png")};

        // Procedural Mob Animations
        zombieWalkRight = new BufferedImage[]{createZombieFrame(size, size, false, 0), createZombieFrame(size, size, false, 1)};
        zombieWalkLeft = new BufferedImage[]{createZombieFrame(size, size, true, 0), createZombieFrame(size, size, true, 1)};
        cowWalkRight = new BufferedImage[]{createCowFrame(size, size, false, 0), createCowFrame(size, size, false, 1)};
        cowWalkLeft = new BufferedImage[]{createCowFrame(size, size, true, 0), createCowFrame(size, size, true, 1)};
        chickenWalkRight = new BufferedImage[]{createChickenFrame(size, size, false, 0), createChickenFrame(size, size, false, 1)};
        chickenWalkLeft = new BufferedImage[]{createChickenFrame(size, size, true, 0), createChickenFrame(size, size, true, 1)};
    }

    private static BufferedImage loadOrGenerate(String filename, Supplier<BufferedImage> proceduralFallback) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    return img;
                }
            } catch (IOException ignored) {
            }
        }
        return proceduralFallback.get();
    }

    private static BufferedImage load(String filename) {
        return loadOrGenerate(filename, () -> {
            System.err.println("Spritesheet not found, using generic texture: " + filename);
            return genericTileSprite;
        });
    }

    private static BufferedImage[] loadSpriteSheet(String filename, int frameWidth, int frameHeight) {
        File file = new File(filename);
        if (file.exists()) {
            try {
                BufferedImage sheet = ImageIO.read(file);
                if (sheet != null) {
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
                }
            } catch (IOException ignored) {
            }
        }
        System.err.println("Spritesheet not found, using generic texture: " + filename);
        return new BufferedImage[]{genericTileSprite};
    }

    private static void initFurnaceSprites(int size) {
        BufferedImage[] frames = loadSpriteSheet("src/main/resources/image/tiles/furnaceTile.png", size, size);
        if (frames.length <= 1) {
            furnaceOffSprite = frames[0];
            furnaceOnFrames = new BufferedImage[]{frames[0]};
        } else {
            long firstWarmth = calculateWarmthScore(frames[0]);
            long lastWarmth = calculateWarmthScore(frames[frames.length - 1]);
            if (firstWarmth <= lastWarmth) {
                furnaceOffSprite = frames[0];
                furnaceOnFrames = Arrays.copyOfRange(frames, 1, frames.length);
            } else {
                furnaceOffSprite = frames[frames.length - 1];
                furnaceOnFrames = Arrays.copyOfRange(frames, 0, frames.length - 1);
            }
        }
        TileType.FURNACE.setSprite(furnaceOffSprite);
    }

    private static long calculateWarmthScore(BufferedImage img) {
        long score = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                score += Math.max(0, r - b) * 3L + Math.max(0, g - b) * 2L + (r + g + b);
            }
        }
        return score;
    }

    private static BufferedImage createMeatTexture(int w, int h, Color primary, Color fatAccent) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(primary.darker());
        g.fillRoundRect(2, 4, w - 4, h - 7, 5, 5);
        g.setColor(primary);
        g.fillRoundRect(3, 5, w - 6, h - 9, 4, 4);
        g.setColor(fatAccent);
        g.fillRect(4, 6, 3, 2);
        g.fillRect(9, 8, 2, 2);
        g.dispose();
        return img;
    }

    private static BufferedImage createZombieFrame(int w, int h, boolean flipLeft, int step) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color skin = new Color(78, 138, 75);
        Color shirt = new Color(45, 110, 125);
        Color pants = new Color(55, 50, 95);

        // Head
        g.setColor(skin);
        g.fillRect(4, 1, 8, 5);
        g.setColor(new Color(20, 25, 20));
        int eyeX = flipLeft ? 5 : 9;
        g.fillRect(eyeX, 3, 2, 1);

        // Torso & outstretched zombie arms
        g.setColor(shirt);
        g.fillRect(5, 6, 6, 5);
        g.setColor(skin);
        int armX = flipLeft ? 1 : 9;
        g.fillRect(armX, 6, 6, 2);

        // Legs with step animation
        g.setColor(pants);
        int legOffset = (step == 0) ? -1 : 1;
        g.fillRect(5 + legOffset, 11, 2, 5);
        g.fillRect(9 - legOffset, 11, 2, 5);
        g.dispose();
        return img;
    }

    private static BufferedImage createCowFrame(int w, int h, boolean flipLeft, int step) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color coat = new Color(95, 62, 38);
        Color patches = new Color(235, 235, 230);
        Color snout = new Color(225, 175, 165);

        // Body
        g.setColor(coat);
        g.fillRoundRect(2, 5, 12, 7, 3, 3);
        g.setColor(patches);
        g.fillRect(5, 6, 3, 3);
        g.fillRect(9, 8, 3, 2);

        // Head
        int headX = flipLeft ? 0 : 10;
        g.setColor(coat.darker());
        g.fillRect(headX, 3, 6, 5);
        g.setColor(snout);
        g.fillRect(flipLeft ? 0 : 13, 6, 3, 2);

        // Legs
        g.setColor(coat.darker());
        int shift = (step == 0) ? 0 : 1;
        g.fillRect(3 + shift, 12, 2, 4);
        g.fillRect(11 - shift, 12, 2, 4);
        g.dispose();
        return img;
    }

    private static BufferedImage createChickenFrame(int w, int h, boolean flipLeft, int step) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color feathers = new Color(245, 245, 248);
        Color beak = new Color(235, 175, 35);
        Color comb = new Color(215, 45, 45);

        // Body
        g.setColor(feathers);
        g.fillRoundRect(4, 6, 8, 6, 3, 3);

        // Head, Comb & Beak
        int headX = flipLeft ? 3 : 9;
        g.fillRect(headX, 3, 4, 4);
        g.setColor(comb);
        g.fillRect(headX + 1, 2, 2, 1);
        g.setColor(beak);
        g.fillRect(flipLeft ? 1 : 13, 5, 2, 2);

        // Legs
        int shift = (step == 0) ? 0 : 1;
        g.fillRect(6 - shift, 12, 1, 3);
        g.fillRect(9 + shift, 12, 1, 3);
        g.dispose();
        return img;
    }

    private static BufferedImage createDarkenedTexture(BufferedImage source, float brightnessFactor) {
        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = source.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;
                int r = Math.clamp((int) (((argb >>> 16) & 0xFF) * brightnessFactor), 0, 255);
                int g = Math.clamp((int) (((argb >>> 8) & 0xFF) * brightnessFactor), 0, 255);
                int b = Math.clamp((int) ((argb & 0xFF) * brightnessFactor), 0, 255);
                result.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static BufferedImage createStickTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color stickDark = new Color(92, 60, 28);
        Color stickLight = new Color(145, 98, 48);
        for (int i = 3; i < 13; i++) {
            int x = i;
            int y = 15 - i;
            g.setColor(stickDark);
            g.fillRect(x, y, 2, 2);
            g.setColor(stickLight);
            g.fillRect(x, y, 1, 1);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage createToolTexture(int w, int h, ToolCategory category, ToolTier tier) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color handleDark = new Color(92, 60, 28);
        Color handleLight = new Color(140, 94, 46);
        Color headBase = tier.getThemeColor();
        Color headDark = headBase.darker();
        Color headShine = new Color(
                Math.min(255, headBase.getRed() + 35),
                Math.min(255, headBase.getGreen() + 35),
                Math.min(255, headBase.getBlue() + 35)
        );
        int handleEnd = (category == ToolCategory.SWORD) ? 6 : 11;
        for (int i = 2; i <= handleEnd; i++) {
            int x = i;
            int y = 15 - i;
            g.setColor(handleDark);
            g.fillRect(x, y, 2, 2);
            g.setColor(handleLight);
            g.fillRect(x, y, 1, 1);
        }
        switch (category) {
            case PICKAXE -> {
                g.setColor(headDark);
                g.fillRect(7, 1, 6, 3);
                g.fillRect(12, 2, 3, 6);
                g.fillRect(5, 2, 2, 2);
                g.fillRect(11, 8, 2, 2);
                g.setColor(headBase);
                g.fillRect(7, 2, 5, 2);
                g.fillRect(12, 3, 2, 5);
                g.setColor(headShine);
                g.fillRect(8, 2, 3, 1);
                g.fillRect(12, 3, 1, 3);
            }
            case AXE -> {
                g.setColor(headDark);
                g.fillRect(7, 1, 6, 6);
                g.fillRect(6, 2, 2, 5);
                g.fillRect(12, 6, 2, 2);
                g.setColor(headBase);
                g.fillRect(7, 2, 5, 4);
                g.fillRect(7, 3, 2, 3);
                g.setColor(headShine);
                g.fillRect(7, 2, 3, 1);
                g.fillRect(7, 3, 1, 3);
            }
            case SHOVEL -> {
                g.setColor(headDark);
                g.fillRect(9, 1, 6, 6);
                g.setColor(headBase);
                g.fillRect(10, 2, 4, 4);
                g.setColor(headShine);
                g.fillRect(11, 2, 2, 1);
                g.fillRect(13, 3, 1, 2);
            }
            case SWORD -> {
                g.setColor(headDark);
                g.fillRect(4, 7, 5, 2);
                g.fillRect(6, 6, 2, 5);
                for (int i = 6; i <= 13; i++) {
                    int x = i;
                    int y = 15 - i;
                    g.setColor(headDark);
                    g.fillRect(x - 1, y - 1, 3, 3);
                }
                for (int i = 6; i <= 13; i++) {
                    int x = i;
                    int y = 15 - i;
                    g.setColor(headBase);
                    g.fillRect(x, y - 1, 2, 2);
                    g.setColor(headShine);
                    g.fillRect(x, y - 1, 1, 1);
                }
            }
            default -> {
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage createOreTexture(BufferedImage baseStone, Color primary, Color highlight) {
        int w = baseStone.getWidth();
        int h = baseStone.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.drawImage(baseStone, 0, 0, null);
        int[][] spots = {{3, 3}, {4, 3}, {3, 4}, {10, 4}, {11, 5}, {5, 10}, {6, 10}, {5, 11}, {11, 11}, {12, 10}};
        for (int i = 0; i < spots.length; i++) {
            g.setColor((i % 3 == 0) ? highlight : primary);
            g.fillRect(spots[i][0], spots[i][1], 2, 2);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage createLogTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color barkDark = new Color(92, 58, 30);
        Color barkMid = new Color(118, 76, 40);
        Color barkLight = new Color(138, 92, 50);
        g.setColor(barkMid);
        g.fillRect(2, 0, w - 4, h);
        g.setColor(barkDark);
        g.fillRect(2, 0, 2, h);
        g.fillRect(w - 4, 0, 2, h);
        g.fillRect(7, 0, 2, h);
        g.setColor(barkLight);
        g.fillRect(5, 2, 1, h - 4);
        g.fillRect(10, 3, 1, h - 5);
        g.dispose();
        return img;
    }

    private static BufferedImage createLeavesTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color leafDark = new Color(34, 105, 42, 235);
        Color leafMid = new Color(48, 138, 56, 240);
        Color leafLight = new Color(72, 168, 76, 240);
        g.setColor(leafMid);
        g.fillRoundRect(1, 1, w - 2, h - 2, 4, 4);
        for (int y = 2; y < h - 2; y += 3) {
            for (int x = 2; x < w - 2; x += 3) {
                g.setColor(((x + y) % 2 == 0) ? leafLight : leafDark);
                g.fillRect(x, y, 2, 2);
            }
        }
        g.dispose();
        return img;
    }

    private static BufferedImage createPlanksTexture(int w, int h, boolean isBackground) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color base = isBackground ? new Color(115, 82, 48) : new Color(168, 122, 74);
        Color border = isBackground ? new Color(78, 52, 28) : new Color(120, 84, 48);
        g.setColor(base);
        g.fillRect(0, 0, w, h);
        g.setColor(border);
        for (int y = 0; y < h; y += 4) {
            g.drawLine(0, y, w, y);
            int jointX = ((y / 4) % 2 == 0) ? 7 : 12;
            g.drawLine(jointX, y, jointX, y + 3);
        }
        g.dispose();
        return img;
    }

    private static BufferedImage createCoalItemTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(30, 30, 35));
        g.fillOval(3, 3, w - 6, h - 6);
        g.setColor(new Color(70, 70, 80));
        g.fillRect(5, 5, 3, 2);
        g.dispose();
        return img;
    }

    private static BufferedImage createIngotTexture(int w, int h, Color base, Color shine) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(base.darker());
        g.fillRoundRect(2, 5, w - 4, h - 9, 3, 3);
        g.setColor(base);
        g.fillRect(3, 6, w - 6, h - 11);
        g.setColor(shine);
        g.drawLine(4, 7, w - 5, 7);
        g.dispose();
        return img;
    }

    private static BufferedImage createChestTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(145, 90, 40));
        g.fillRect(1, 2, w - 2, h - 3);
        g.setColor(new Color(80, 45, 18));
        g.drawRect(1, 2, w - 3, h - 4);
        g.drawLine(1, 7, w - 2, 7);
        g.setColor(new Color(235, 200, 50));
        g.fillRect(7, 6, 2, 3);
        g.dispose();
        return img;
    }

    private static BufferedImage createFurnaceTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(105, 105, 110));
        g.fillRect(1, 1, w - 2, h - 2);
        g.setColor(new Color(55, 55, 60));
        g.drawRect(1, 1, w - 3, h - 3);
        g.setColor(new Color(30, 30, 35));
        g.fillRect(4, 4, w - 8, 4);
        g.setColor(new Color(255, 120, 20));
        g.fillRect(4, 10, w - 8, 4);
        g.dispose();
        return img;
    }

    private static BufferedImage createCraftingTableTexture(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(155, 110, 65));
        g.fillRect(1, 4, w - 2, h - 5);
        g.setColor(new Color(195, 140, 85));
        g.fillRect(0, 1, w, 4);
        g.setColor(new Color(95, 60, 30));
        g.drawRect(0, 1, w - 1, 4);
        g.drawRect(1, 5, w - 3, h - 6);
        g.dispose();
        return img;
    }

    private static BufferedImage createGenericTexture(int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Color c1 = new Color(180, 0, 220);
        Color c2 = new Color(35, 35, 35);
        for (int y = 0; y < height; y += 4) {
            for (int x = 0; x < width; x += 4) {
                g.setColor(((x / 4 + y / 4) % 2 == 0) ? c1 : c2);
                g.fillRect(x, y, 4, 4);
            }
        }
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, width - 1, height - 1);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("?", 5, 12);
        g.dispose();
        return img;
    }
}