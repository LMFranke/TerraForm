package br.lucasfranke.model.type;

import br.lucasfranke.Sprite;
import br.lucasfranke.sound.Sound;

import java.awt.image.BufferedImage;

public enum TileType {

    AIR(false, false, false, false, 0, 0),
    GRASS(true, true, true, false, 0, 55),
    DIRT(true, true, true, false, 0, 55),
    STONE(true, true, true, false, 0, 155),
    WOOD(true, true, true, false, 0, 75),
    LEAVES(false, true, true, false, 0, 20),
    PLANKS(true, true, true, false, 0, 65),

    COAL_ORE(true, true, true, false, 0, 170),
    IRON_ORE(true, true, true, false, 0, 210),
    GOLD_ORE(true, true, true, false, 0, 260),

    BACKGROUND_DIRT(false, false, true, false, 0, 45),
    BACKGROUND_GRASS(false, false, true, false, 0, 45),
    BACKGROUND_STONE(false, false, true, false, 0, 110),
    BACKGROUND_PLANKS(false, false, true, false, 0, 50),
    BACKGROUND_WOOD(false, false, true, false, 0, 75),
    BACKGROUND_LEAVES(false, false, true, false, 0, 20),

    TORCH(false, true, true, false, 14, 1),
    CHEST(false, true, true, true, 0, 60),
    FURNACE(false, true, true, true, 10, 90),
    CRAFTING_TABLE(false, true, true, true, 0, 60),


    STICK(false, false, false, false, 0, 0),
    COAL(false, false, false, false, 0, 0),
    IRON_INGOT(false, false, false, false, 0, 0),
    GOLD_INGOT(false, false, false, false, 0, 0),

    WOODEN_PICKAXE(false, false, false, false, 0, 0, ToolCategory.PICKAXE, ToolTier.WOOD),
    STONE_PICKAXE(false, false, false, false, 0, 0, ToolCategory.PICKAXE, ToolTier.STONE),
    IRON_PICKAXE(false, false, false, false, 0, 0, ToolCategory.PICKAXE, ToolTier.IRON),
    GOLD_PICKAXE(false, false, false, false, 0, 0, ToolCategory.PICKAXE, ToolTier.GOLD),


    WOODEN_AXE(false, false, false, false, 0, 0, ToolCategory.AXE, ToolTier.WOOD),
    STONE_AXE(false, false, false, false, 0, 0, ToolCategory.AXE, ToolTier.STONE),
    IRON_AXE(false, false, false, false, 0, 0, ToolCategory.AXE, ToolTier.IRON),
    GOLD_AXE(false, false, false, false, 0, 0, ToolCategory.AXE, ToolTier.GOLD),


    WOODEN_SHOVEL(false, false, false, false, 0, 0, ToolCategory.SHOVEL, ToolTier.WOOD),
    STONE_SHOVEL(false, false, false, false, 0, 0, ToolCategory.SHOVEL, ToolTier.STONE),
    IRON_SHOVEL(false, false, false, false, 0, 0, ToolCategory.SHOVEL, ToolTier.IRON),
    GOLD_SHOVEL(false, false, false, false, 0, 0, ToolCategory.SHOVEL, ToolTier.GOLD),


    WOODEN_SWORD(false, false, false, false, 0, 0, ToolCategory.SWORD, ToolTier.WOOD),
    STONE_SWORD(false, false, false, false, 0, 0, ToolCategory.SWORD, ToolTier.STONE),
    IRON_SWORD(false, false, false, false, 0, 0, ToolCategory.SWORD, ToolTier.IRON),
    GOLD_SWORD(false, false, false, false, 0, 0, ToolCategory.SWORD, ToolTier.GOLD),


    RAW_BEEF(false, false, false, false, 0, 0),
    COOKED_BEEF(false, false, false, false, 0, 0),
    RAW_CHICKEN(false, false, false, false, 0, 0),
    COOKED_CHICKEN(false, false, false, false, 0, 0),
    ROTTEN_FLESH(false, false, false, false, 0, 0);

    private final boolean solid;
    private final boolean placeable;
    private final boolean breakable;
    private final boolean functional;
    private final int lightEmission;
    private final int hardness;
    private final ToolCategory toolCategory;
    private final ToolTier toolTier;

    private BufferedImage sprite;
    private Sprite animation;

    TileType(boolean solid, boolean placeable, boolean breakable, boolean functional, int lightEmission, int hardness) {
        this(solid, placeable, breakable, functional, lightEmission, hardness, ToolCategory.NONE, ToolTier.NONE);
    }

    TileType(boolean solid, boolean placeable, boolean breakable, boolean functional, int lightEmission, int hardness,
             ToolCategory toolCategory, ToolTier toolTier) {
        this.solid = solid;
        this.placeable = placeable;
        this.breakable = breakable;
        this.functional = functional;
        this.lightEmission = lightEmission;
        this.hardness = hardness;
        this.toolCategory = toolCategory;
        this.toolTier = toolTier;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isPlaceable() {
        return placeable;
    }

    public boolean isBreakable() {
        return breakable;
    }

    public boolean isFunctional() {
        return functional;
    }

    public int getLightEmission() {
        return lightEmission;
    }

    public int getHardness() {
        return hardness;
    }

    public ToolCategory getToolCategory() {
        return toolCategory;
    }

    public ToolTier getToolTier() {
        return toolTier;
    }

    public boolean isTool() {
        return toolCategory != ToolCategory.NONE && toolTier != ToolTier.NONE;
    }

    public boolean isFood() {
        return getFoodHealAmount() > 0;
    }

    public int getFoodHealAmount() {
        return switch (this) {
            case RAW_BEEF, RAW_CHICKEN -> 3;
            case COOKED_BEEF -> 8;
            case COOKED_CHICKEN -> 6;
            case ROTTEN_FLESH -> 2;
            default -> 0;
        };
    }

    public int getAttackDamage() {
        if (!isTool()) {
            return 2;
        }
        int tierBonus = switch (toolTier) {
            case WOOD -> 2;
            case GOLD -> 3;
            case STONE -> 4;
            case IRON -> 6;
            default -> 0;
        };
        return switch (toolCategory) {
            case SWORD -> 3 + tierBonus;
            case AXE -> 2 + tierBonus;
            case PICKAXE -> 1 + tierBonus;
            case SHOVEL -> 1 + Math.max(1, tierBonus - 1);
            default -> 2;
        };
    }

    public ToolCategory getPreferredTool() {
        return switch (this) {
            case STONE, BACKGROUND_STONE, COAL_ORE, IRON_ORE, GOLD_ORE, FURNACE -> ToolCategory.PICKAXE;
            case WOOD, BACKGROUND_WOOD, PLANKS, BACKGROUND_PLANKS, CHEST, CRAFTING_TABLE -> ToolCategory.AXE;
            case GRASS, DIRT, BACKGROUND_GRASS, BACKGROUND_DIRT -> ToolCategory.SHOVEL;
            case LEAVES, BACKGROUND_LEAVES -> ToolCategory.SWORD;
            default -> ToolCategory.NONE;
        };
    }

    public ToolTier getRequiredTier() {
        return switch (this) {
            case STONE, BACKGROUND_STONE, COAL_ORE, FURNACE -> ToolTier.WOOD;
            case IRON_ORE -> ToolTier.STONE;
            case GOLD_ORE -> ToolTier.IRON;
            default -> ToolTier.NONE;
        };
    }

    public TileType getDropType() {
        return switch (this) {
            case GRASS, BACKGROUND_GRASS -> GRASS;
            case DIRT, BACKGROUND_DIRT -> DIRT;
            case STONE, BACKGROUND_STONE -> STONE;
            case WOOD, BACKGROUND_WOOD -> WOOD;
            case LEAVES, BACKGROUND_LEAVES -> LEAVES;
            case PLANKS, BACKGROUND_PLANKS -> PLANKS;
            case COAL_ORE -> COAL;
            case IRON_ORE -> IRON_ORE;
            case GOLD_ORE -> GOLD_ORE;
            case TORCH -> TORCH;
            case CHEST -> CHEST;
            case FURNACE -> FURNACE;
            case CRAFTING_TABLE -> CRAFTING_TABLE;
            default -> AIR;
        };
    }

    public TileType getBackgroundVariant() {
        return switch (this) {
            case GRASS -> BACKGROUND_GRASS;
            case DIRT -> BACKGROUND_DIRT;
            case STONE -> BACKGROUND_STONE;
            case PLANKS -> BACKGROUND_PLANKS;
            case WOOD -> BACKGROUND_WOOD;
            case LEAVES -> BACKGROUND_LEAVES;
            default -> AIR;
        };
    }

    public Sound getBreakSound() {
        return switch (this) {
            case DIRT, GRASS, WOOD, LEAVES, PLANKS,
                 BACKGROUND_DIRT, BACKGROUND_GRASS, BACKGROUND_PLANKS,
                 TORCH, CHEST, CRAFTING_TABLE
                    -> Sound.GRASS_BREAK;
            case STONE, COAL_ORE, IRON_ORE, GOLD_ORE, BACKGROUND_STONE, FURNACE
                    -> Sound.STONE_BREAK;
            default -> null;
        };
    }

    public BufferedImage getSprite() {
        if (animation != null) {
            return animation.getFrame();
        }
        return sprite;
    }

    public void setSprite(BufferedImage sprite) {
        this.sprite = sprite;
    }

    public void setAnimation(BufferedImage[] frames, int frameDelay) {
        if (frames != null && frames.length > 0) {
            this.sprite = frames[0];
            this.animation = new Sprite(frames, frameDelay);
        }
    }

    public static void updateAnimations() {
        for (TileType type : values()) {
            if (type.animation != null) {
                type.animation.update();
            }
        }
    }
}