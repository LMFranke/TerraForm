package br.lucasfranke.model.tile;

public class BackgroundDirtTile extends Tile {

    public BackgroundDirtTile(int id) {
        super(id);
        isCollided = false;
        this.imageName = "backgroundDirtTile";
        loadImage();
    }

}
