package br.lucasfranke.model.block;

import br.lucasfranke.model.type.TileType;

public class ChestBlock extends FunctionalBlock {

    public ChestBlock(int worldX, int worldY) {
        super(worldX, worldY, TileType.CHEST, "Storage Chest", 3, 9);
    }
}