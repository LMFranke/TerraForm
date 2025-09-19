package br.lucasfranke.model.tile;

public class GrassTile extends Tile {

    public GrassTile(int id) {
        super(id);
        isCollided = true;
        imageName = "grassTile";
        loadImage();
    }

}
