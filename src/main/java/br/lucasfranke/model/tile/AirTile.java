package br.lucasfranke.model.tile;

public class AirTile extends Tile {

    public AirTile(int id) {
        super(id);
        imageName = "skyTile";
        isCollided = false;
        loadImage();
    }

}
