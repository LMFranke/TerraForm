package br.lucasfranke.model.tile;

import br.lucasfranke.model.type.Direction;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Objects;

public class Tile {

    protected int id;
    protected BufferedImage image;
    protected String imageName;
    protected boolean isBackground;

    protected boolean isCollided;
    protected Direction currentDirection;
    protected int x;
    protected int y;

    public Tile(int id) {
        this.id = id;
    }

    public void loadImage() {
        try {
            this.image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/image/tiles/" + imageName + ".png")));
        } catch (Exception e) {
            System.out.println("Error loading " + getClass().getName() + " image");
        }
    }

    public boolean isBackground() {
        return isBackground;
    }

    public void setBackground(boolean background) {
        isBackground = background;
    }

    public BufferedImage getImage() {
        return image;
    }

    public boolean isCollided() {
        return isCollided;
    }

    public Direction getCurrentDirection() {
        return currentDirection;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setX(int x) {
        this.x = x;
    }

    @Override
    public String toString() {
        return getClass().getName() + "{" + "x=" + x + ", y=" + y + '}';
    }
}
