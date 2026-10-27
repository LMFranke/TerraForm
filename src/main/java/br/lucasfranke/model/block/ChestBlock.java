package br.lucasfranke.model.block;

import br.lucasfranke.model.type.TileType;

public class ChestBlock extends FunctionalBlock {

    public ChestBlock(int worldX, int worldY) {
        super(worldX, worldY, TileType.CHEST, "Chest", 3, 9);
    }
}