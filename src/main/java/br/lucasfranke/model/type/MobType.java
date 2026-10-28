package br.lucasfranke.model.type;

public enum MobType {
    ZOMBIE("Zombie", 20, 2.1, 40, 46, true, 4, TileType.ROTTEN_FLESH, 1, 2),
    COW("Cow", 12, 1.4, 46, 36, false, 0, TileType.RAW_BEEF, 1, 3),
    CHICKEN("Chicken", 6, 1.8, 30, 30, false, 0, TileType.RAW_CHICKEN, 1, 2);

    private final String displayName;
    private final int maxHealth;
    private final double baseSpeed;
    private final int width;
    private final int height;
    private final boolean hostile;
    private final int attackDamage;
    private final TileType dropItem;
    private final int minDrop;
    private final int maxDrop;

    MobType(String displayName, int maxHealth, double baseSpeed, int width, int height,
            boolean hostile, int attackDamage, TileType dropItem, int minDrop, int maxDrop) {
        this.displayName = displayName;
        this.maxHealth = maxHealth;
        this.baseSpeed = baseSpeed;
        this.width = width;
        this.height = height;
        this.hostile = hostile;
        this.attackDamage = attackDamage;
        this.dropItem = dropItem;
        this.minDrop = minDrop;
        this.maxDrop = maxDrop;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public double getBaseSpeed() {
        return baseSpeed;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isHostile() {
        return hostile;
    }

    public int getAttackDamage() {
        return attackDamage;
    }

    public TileType getDropItem() {
        return dropItem;
    }

    public int getMinDrop() {
        return minDrop;
    }

    public int getMaxDrop() {
        return maxDrop;
    }
}