package br.lucasfranke.world.render;

import br.lucasfranke.graphics.Assets;
import br.lucasfranke.EnginePanel;
import br.lucasfranke.world.World;
import br.lucasfranke.world.generation.Noise1D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class ParallaxBackground {

    private static final class CloudInstance {
        double worldX;
        int screenOffsetY;
        double speed;
        double parallaxFactor;
        BufferedImage[] shadedFrames;
        int drawW;
        int drawH;
    }

    private static final int CLOUD_COUNT = 8;
    private static final int CLOUD_SPAN_WIDTH = EnginePanel.WIDTH + 600;

    private static final double FAR_MOUNTAIN_SCALE = 1.35;
    private static final double NEAR_MOUNTAIN_SCALE = 1.40;

    private static final float FAR_ATMOSPHERIC_HAZE = 0.58f;
    private static final float NEAR_ATMOSPHERIC_HAZE = 0.48f;

    private static final double SURFACE_REFERENCE_Y = 36.0 * EnginePanel.tileSquare;
    private static final int BASE_FAR_Y = (int) Math.round(EnginePanel.HEIGHT * 0.25);
    private static final int BASE_NEAR_Y = (int) Math.round(EnginePanel.HEIGHT * 0.25);
    private static final int BASE_FOOTHILLS_Y = (int) Math.round(EnginePanel.HEIGHT * 0.44);

    private final CloudInstance[] clouds = new CloudInstance[CLOUD_COUNT];

    private final BufferedImage[] farMountainsShaded;
    private final BufferedImage[] nearMountainsShaded;
    private final BufferedImage[] foothillsShaded;

    private final Color[] farFillColors = new Color[16];
    private final Color[] nearFillColors = new Color[16];
    private final Color[] foothillsFillColors = new Color[16];
    private final Color[] starColors = new Color[16];

    private final Color farBaseColor = new Color(110, 148, 205);
    private final Color nearBaseColor = new Color(78, 102, 152);
    private final Color foothillsBaseColor = new Color(36, 72, 58);

    private final BufferedImage sunSprite;
    private final BufferedImage moonSprite;

    private final int[] starX;
    private final int[] starY;
    private final int[] starSize;

    private GradientPaint cachedSkyGradient;
    private int lastGradientSunLight = -1;
    private int lastGradientTimeBucket = -1;

    private double smoothedCameraY = Double.NaN;

    public ParallaxBackground(long seed) {
        this.sunSprite = createScaledCelestialSprite(createSunSprite());
        this.moonSprite = createScaledCelestialSprite(createMoonSprite());

        int starCount = 75;
        this.starX = new int[starCount];
        this.starY = new int[starCount];
        this.starSize = new int[starCount];
        generateStars(seed + 404, starCount);

        precomputeLightingColors();
        initClouds(seed + 101);

        Color skyHazeColor = new Color(125, 180, 245);

        if (Assets.bgMountainsFar != null) {
            BufferedImage processedFar = applyAtmosphericHazeAndBaseFade(
                    Assets.bgMountainsFar, skyHazeColor, farBaseColor, FAR_ATMOSPHERIC_HAZE);
            this.farMountainsShaded = buildPreScaledShadedTable(processedFar, FAR_MOUNTAIN_SCALE);
        } else {
            this.farMountainsShaded = null;
        }

        if (Assets.bgMountainsNear != null) {
            BufferedImage processedNear = applyAtmosphericHazeAndBaseFade(
                    Assets.bgMountainsNear, skyHazeColor, nearBaseColor, NEAR_ATMOSPHERIC_HAZE);
            this.nearMountainsShaded = buildPreScaledShadedTable(processedNear, NEAR_MOUNTAIN_SCALE);
        } else {
            this.nearMountainsShaded = null;
        }

        BufferedImage rawFoothills = generateFoothillsLayer(seed + 505, 320, 110);
        this.foothillsShaded = buildPreScaledShadedTable(rawFoothills, EnginePanel.scale);
    }

    private void precomputeLightingColors() {
        Color nightShadow = new Color(8, 12, 24);
        for (int light = 0; light < 16; light++) {
            float shadowFactor = 1.0f - ((1.0f - (light / 15.0f)) * 0.82f);
            farFillColors[light] = blendColors(nightShadow, farBaseColor, shadowFactor);
            nearFillColors[light] = blendColors(nightShadow, nearBaseColor, shadowFactor);
            foothillsFillColors[light] = blendColors(nightShadow, foothillsBaseColor, shadowFactor);

            float dayRatio = (light - 2) / 13.0f;
            if (dayRatio < 0.6f) {
                int starAlpha = Math.clamp((int) ((1.0f - (dayRatio / 0.6f)) * 220), 0, 255);
                starColors[light] = new Color(245, 248, 255, starAlpha);
            } else {
                starColors[light] = null;
            }
        }
    }

    private void initClouds(long seed) {
        Random rand = new Random(seed);
        BufferedImage[] models = Assets.bgCloudModels;

        BufferedImage[][] modelShadedTables = new BufferedImage[models.length][];
        for (int m = 0; m < models.length; m++) {
            modelShadedTables[m] = buildPreScaledShadedTable(models[m], EnginePanel.scale);
        }

        for (int i = 0; i < CLOUD_COUNT; i++) {
            CloudInstance c = new CloudInstance();
            int modelIdx = rand.nextInt(models.length);
            c.shadedFrames = modelShadedTables[modelIdx];
            c.drawW = c.shadedFrames[15].getWidth();
            c.drawH = c.shadedFrames[15].getHeight();
            c.worldX = (i * (CLOUD_SPAN_WIDTH / (double) CLOUD_COUNT)) + rand.nextInt(90);
            c.screenOffsetY = 28 + rand.nextInt(140);
            c.speed = 0.18 + rand.nextDouble() * 0.22;
            c.parallaxFactor = 0.06 + (i % 3) * 0.025;
            clouds[i] = c;
        }
    }

    public void update() {
        for (int i = 0; i < CLOUD_COUNT; i++) {
            clouds[i].worldX += clouds[i].speed;
        }
    }

    public void draw(Graphics2D g2, World world, int cameraWorldX, int cameraWorldY, double timeOfDay) {
        if (Double.isNaN(smoothedCameraY)) {
            smoothedCameraY = cameraWorldY;
        } else if (Math.abs(cameraWorldY - smoothedCameraY) > 6.0) {
            smoothedCameraY += (cameraWorldY - smoothedCameraY) * 0.08;
        }

        int sunLight = Math.clamp(world.getLighting().getSunLightLevel(), 0, 15);
        double cameraDeltaY = Math.clamp(smoothedCameraY - SURFACE_REFERENCE_Y, -600.0, 600.0);

        int farY = BASE_FAR_Y - (int) Math.round(cameraDeltaY * 0.010);
        int nearY = BASE_NEAR_Y - (int) Math.round(cameraDeltaY * 0.015);
        int foothillsY = BASE_FOOTHILLS_Y - (int) Math.round(cameraDeltaY * 0.020);

        BufferedImage farImg = (farMountainsShaded != null) ? farMountainsShaded[sunLight] : null;
        BufferedImage nearImg = (nearMountainsShaded != null) ? nearMountainsShaded[sunLight] : null;
        BufferedImage foothillsImg = foothillsShaded[sunLight];

        int farBottomY = (farImg != null) ? farY + farImg.getHeight() : EnginePanel.HEIGHT;
        int nearBottomY = (nearImg != null) ? nearY + nearImg.getHeight() : EnginePanel.HEIGHT;
        int foothillsBottomY = foothillsY + foothillsImg.getHeight();

        int skyFillHeight = Math.min(EnginePanel.HEIGHT, Math.min(farBottomY, Math.min(nearBottomY, foothillsBottomY)));

        Paint oldPaint = g2.getPaint();
        g2.setPaint(getOrUpdateSkyGradient(sunLight, timeOfDay));
        g2.fillRect(0, 0, EnginePanel.WIDTH, skyFillHeight);
        g2.setPaint(oldPaint);

        // stars
        Color starColor = starColors[sunLight];
        if (starColor != null) {
            g2.setColor(starColor);
            for (int i = 0; i < starX.length; i++) {
                int s = starSize[i];
                g2.fillRect(starX[i], starY[i], s, s);
            }
        }

        // sun/moon
        drawCelestialBody(g2, timeOfDay);

        // clouds
        int cloudBaseY = (int) Math.round(-cameraDeltaY * 0.005);
        for (int i = 0; i < CLOUD_COUNT; i++) {
            CloudInstance c = clouds[i];
            int screenY = c.screenOffsetY + cloudBaseY;
            if (screenY + c.drawH >= 0 && screenY <= skyFillHeight) {
                double relX = c.worldX - (cameraWorldX * c.parallaxFactor);
                int screenX = Math.floorMod((int) Math.round(relX), CLOUD_SPAN_WIDTH) - 200;
                g2.drawImage(c.shadedFrames[sunLight], screenX, screenY, null);
            }
        }

        // far mountains
        if (farImg != null) {
            int nextCoverY = Math.min(nearBottomY, foothillsBottomY);
            drawTiledLayerFast(g2, farImg, cameraWorldX * 0.10, farY, farFillColors[sunLight], nextCoverY);
        }

        // near mountains
        if (nearImg != null) {
            drawTiledLayerFast(g2, nearImg, cameraWorldX * 0.15, nearY, nearFillColors[sunLight], foothillsBottomY);
        }

        drawTiledLayerFast(g2, foothillsImg, cameraWorldX * 0.20, foothillsY, foothillsFillColors[sunLight], EnginePanel.HEIGHT);
    }

    private void drawTiledLayerFast(Graphics2D g2, BufferedImage preScaledLayer, double parallaxX,
                                    int screenY, Color fillColor, int maxFillBottomY) {
        int drawW = preScaledLayer.getWidth();
        int drawH = preScaledLayer.getHeight();
        int bottomY = screenY + drawH;

        int fillStartY = Math.max(0, bottomY - 4);
        int fillEndY = Math.min(EnginePanel.HEIGHT, maxFillBottomY);
        if (fillStartY < fillEndY) {
            g2.setColor(fillColor);
            g2.fillRect(0, fillStartY, EnginePanel.WIDTH, fillEndY - fillStartY);
        }

        if (screenY > EnginePanel.HEIGHT || bottomY < 0) {
            return;
        }

        int offsetX = Math.floorMod((int) Math.round(parallaxX), drawW);
        for (int x = -offsetX; x < EnginePanel.WIDTH; x += drawW) {
            g2.drawImage(preScaledLayer, x, screenY, null);
        }
    }

    private GradientPaint getOrUpdateSkyGradient(int sunLight, double timeOfDay) {
        int timeBucket = ((timeOfDay >= 17.0 && timeOfDay < 20.0) || (timeOfDay >= 4.0 && timeOfDay < 7.0))
                ? (int) (timeOfDay * 30.0)
                : -1;

        if (cachedSkyGradient != null && sunLight == lastGradientSunLight && timeBucket == lastGradientTimeBucket) {
            return cachedSkyGradient;
        }

        lastGradientSunLight = sunLight;
        lastGradientTimeBucket = timeBucket;

        float dayRatio = (sunLight - 2) / 13.0f;
        Color topSky = blendColors(new Color(10, 14, 30), new Color(55, 125, 225), dayRatio);
        Color horizonSky = blendColors(new Color(22, 28, 52), new Color(145, 200, 252), dayRatio);

        if (timeOfDay >= 17.0 && timeOfDay < 20.0) {
            float sunsetFactor = (float) Math.sin(((timeOfDay - 17.0) / 3.0) * Math.PI);
            horizonSky = blendColors(horizonSky, new Color(235, 130, 85), sunsetFactor * 0.65f);
        } else if (timeOfDay >= 4.0 && timeOfDay < 7.0) {
            float dawnFactor = (float) Math.sin(((timeOfDay - 4.0) / 3.0) * Math.PI);
            horizonSky = blendColors(horizonSky, new Color(240, 155, 110), dawnFactor * 0.55f);
        }

        cachedSkyGradient = new GradientPaint(0, 0, topSky, 0, EnginePanel.HEIGHT * 0.72f, horizonSky);
        return cachedSkyGradient;
    }

    private BufferedImage[] buildPreScaledShadedTable(BufferedImage base, double scaleFactor) {
        int scaledW = Math.max(1, (int) Math.round(base.getWidth() * scaleFactor));
        int scaledH = Math.max(1, (int) Math.round(base.getHeight() * scaleFactor));

        BufferedImage scaledBase = createCompatibleTranslucentImage(scaledW, scaledH);
        Graphics2D gBase = scaledBase.createGraphics();
        gBase.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        gBase.drawImage(base, 0, 0, scaledW, scaledH, null);
        gBase.dispose();

        BufferedImage[] table = new BufferedImage[16];
        table[15] = scaledBase;

        for (int idx = 0; idx < 15; idx++) {
            BufferedImage shaded = createCompatibleTranslucentImage(scaledW, scaledH);
            Graphics2D g = shaded.createGraphics();
            g.drawImage(scaledBase, 0, 0, null);
            g.setComposite(AlphaComposite.SrcAtop);
            float shadowAlpha = (1.0f - (idx / 15.0f)) * 0.82f;
            g.setColor(new Color(8, 12, 24, Math.clamp((int) (shadowAlpha * 255), 0, 255)));
            g.fillRect(0, 0, scaledW, scaledH);
            g.dispose();
            table[idx] = shaded;
        }
        return table;
    }

    private BufferedImage createScaledCelestialSprite(BufferedImage raw) {
        int size = 16 * EnginePanel.scale;
        BufferedImage scaled = createCompatibleTranslucentImage(size, size);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(raw, 0, 0, size, size, null);
        g.dispose();
        return scaled;
    }

    private BufferedImage createCompatibleTranslucentImage(int w, int h) {
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            if (!ge.isHeadlessInstance()) {
                GraphicsConfiguration gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
                return gc.createCompatibleImage(w, h, Transparency.TRANSLUCENT);
            }
        } catch (Exception ignored) {
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private BufferedImage applyAtmosphericHazeAndBaseFade(BufferedImage src, Color hazeColor,
                                                          Color bottomTargetColor, float hazeStrength) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int hr = hazeColor.getRed();
        int hg = hazeColor.getGreen();
        int hb = hazeColor.getBlue();

        int br = bottomTargetColor.getRed();
        int bg = bottomTargetColor.getGreen();
        int bb = bottomTargetColor.getBlue();

        int fadeStartRow = (int) (h * 0.68);

        for (int y = 0; y < h; y++) {
            float verticalFactor = (float) y / h;
            float mix = Math.clamp(hazeStrength + verticalFactor * 0.10f, 0f, 0.85f);

            float bottomFade = (y >= fadeStartRow)
                    ? (float) (y - fadeStartRow) / Math.max(1, (h - 1 - fadeStartRow))
                    : 0f;

            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) continue;

                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;

                if (y == 0 || (r > 210 && g > 218 && b > 225 && y < h * 0.35)) {
                    continue;
                }

                int nr = (int) (r + (hr - r) * mix);
                int ng = (int) (g + (hg - g) * mix);
                int nb = (int) (b + (hb - b) * mix);

                if (bottomFade > 0f) {
                    nr = (int) (nr + (br - nr) * bottomFade);
                    ng = (int) (ng + (bg - ng) * bottomFade);
                    nb = (int) (nb + (bb - nb) * bottomFade);
                }

                out.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return out;
    }

    private BufferedImage generateFoothillsLayer(long seed, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Noise1D noise = new Noise1D(seed);

        Color ridgeHighlight = new Color(56, 106, 82);
        Color hillMid = new Color(44, 86, 68);
        Color treeDark = new Color(30, 62, 50);
        Color treeLight = new Color(48, 94, 74);

        int[] hillTopY = new int[w];
        for (int x = 0; x < w; x++) {
            double angle = (x / (double) w) * Math.PI * 2.0;
            double n = noise.sample(Math.cos(angle) * 1.8 + 7.0) * 0.65
                    + noise.sample(Math.sin(angle) * 3.6 + 19.0) * 0.35;
            int topY = (int) (26 + n * 32);
            hillTopY[x] = topY;

            for (int y = topY; y < h; y++) {
                float fade = Math.clamp((float) (y - topY) / (h - topY), 0f, 1f);
                Color body = blendColors(hillMid, foothillsBaseColor, fade);
                g.setColor((y - topY < 3) ? ridgeHighlight : body);
                g.fillRect(x, y, 1, 1);
            }
        }

        Random rand = new Random(seed);
        for (int x = 4; x < w - 4; x += 4 + rand.nextInt(5)) {
            int baseY = hillTopY[x];
            int treeH = 7 + rand.nextInt(6);
            int topY = baseY - treeH;
            for (int y = topY; y <= baseY; y++) {
                int progress = y - topY;
                int halfW = (progress / 3) + (progress % 2 == 0 ? 1 : 0);
                for (int dx = -halfW; dx <= halfW; dx++) {
                    int px = Math.floorMod(x + dx, w);
                    g.setColor(dx < 0 ? treeLight : treeDark);
                    g.fillRect(px, y, 1, 1);
                }
            }
        }

        g.dispose();
        return img;
    }

    private void drawCelestialBody(Graphics2D g2, double timeOfDay) {
        boolean isDay = (timeOfDay >= 6.0 && timeOfDay < 18.0);
        double phase = isDay ? (timeOfDay - 6.0) / 12.0 : ((timeOfDay + 6.0) % 24.0) / 12.0;

        int margin = 120;
        int x = (int) (margin + phase * (EnginePanel.WIDTH - margin * 2));
        int arcHeight = (int) (Math.sin(phase * Math.PI) * (EnginePanel.HEIGHT * 0.26));
        int y = (int) (EnginePanel.HEIGHT * 0.34) - arcHeight;

        BufferedImage body = isDay ? sunSprite : moonSprite;
        int halfSize = body.getWidth() / 2;
        g2.drawImage(body, x - halfSize, y - halfSize, null);
    }

    private BufferedImage createSunSprite() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(255, 220, 75, 90));
        g.fillRect(2, 2, 12, 12);
        g.setColor(new Color(255, 235, 110));
        g.fillRect(4, 4, 8, 8);
        g.setColor(new Color(255, 250, 200));
        g.fillRect(5, 5, 6, 6);
        g.dispose();
        return img;
    }

    private BufferedImage createMoonSprite() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(200, 215, 255, 70));
        g.fillRect(3, 3, 10, 10);
        g.setColor(new Color(230, 238, 255));
        g.fillRect(4, 4, 7, 7);
        g.setColor(new Color(185, 198, 225));
        g.fillRect(5, 6, 2, 2);
        g.fillRect(8, 8, 2, 2);
        g.dispose();
        return img;
    }

    private void generateStars(long seed, int count) {
        Random rand = new Random(seed);
        for (int i = 0; i < count; i++) {
            starX[i] = rand.nextInt(EnginePanel.WIDTH);
            starY[i] = rand.nextInt((int) (EnginePanel.HEIGHT * 0.45));
            starSize[i] = (rand.nextInt(4) == 0) ? EnginePanel.scale : EnginePanel.scale - 1;
        }
    }

    private Color blendColors(Color c1, Color c2, float ratio) {
        float r = Math.clamp(ratio, 0f, 1f);
        int red = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * r);
        int green = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * r);
        int blue = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * r);
        return new Color(red, green, blue);
    }
}