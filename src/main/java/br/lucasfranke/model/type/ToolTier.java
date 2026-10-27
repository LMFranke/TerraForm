package br.lucasfranke.model.type;

import java.awt.Color;

public enum ToolTier {
    NONE(0, 1.0f, 0, new Color(200, 200, 200)),
    WOOD(1, 2.0f, 60, new Color(160, 110, 60)),
    STONE(2, 3.5f, 132, new Color(130, 130, 135)),
    IRON(3, 5.5f, 250, new Color(215, 215, 225)),
    GOLD(2, 8.5f, 40, new Color(245, 205, 45)); // High speed, low durability

    private final int harvestLevel;
    private final float efficiencyMultiplier;
    private final int maxDurability;
    private final Color themeColor;

    ToolTier(int harvestLevel, float efficiencyMultiplier, int maxDurability, Color themeColor) {
        this.harvestLevel = harvestLevel;
        this.efficiencyMultiplier = efficiencyMultiplier;
        this.maxDurability = maxDurability;
        this.themeColor = themeColor;
    }

    public int getHarvestLevel() {
        return harvestLevel;
    }

    public float getEfficiencyMultiplier() {
        return efficiencyMultiplier;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public Color getThemeColor() {
        return themeColor;
    }

    public boolean canHarvest(ToolTier minimumRequired) {
        return this.harvestLevel >= minimumRequired.harvestLevel;
    }
}