package br.lucasfranke.model.tile;

public class DirtTile extends Tile {

    public DirtTile(int id) {
        super(id);
        isCollided = true;
        imageName = "dirtTile";
        loadImage();
    }

}
