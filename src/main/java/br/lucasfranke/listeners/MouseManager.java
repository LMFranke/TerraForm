package br.lucasfranke.listeners;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.util.HashSet;
import java.util.Set;

public class MouseManager extends MouseAdapter implements MouseMotionListener {

    private static int x = 0;
    private static int y = 0;
    private static int wheelRotation = 0;

    private static final Set<Integer> pressedMouse = new HashSet<>();


    private long lastClickTime = 0;
    private final long clickCooldown = 25;

    public static int getX() {
        return x;
    }

    public static int getY() {
        return y;
    }

    public static int getWheelRotation() {
        return wheelRotation;
    }

    public static void setWheelRotation(int value) {
        wheelRotation = value;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        super.mousePressed(e);
        if (lastClickTime + clickCooldown < System.currentTimeMillis()) {
            pressedMouse.add(e.getButton());
            lastClickTime = System.currentTimeMillis();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        super.mouseReleased(e);
        pressedMouse.remove(e.getButton());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        super.mouseDragged(e);
        x = e.getX();
        y = e.getY();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        super.mouseMoved(e);
        x = e.getX();
        y = e.getY();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        super.mouseWheelMoved(e);
        wheelRotation = e.getWheelRotation();
    }

    public static boolean isButtonPressed(int buttonCode) {
        return pressedMouse.contains(buttonCode);
    }

    public static void unpressButton(int buttonCode) {
        pressedMouse.remove(buttonCode);
    }

    public static void clearKeys() {
        pressedMouse.clear();
    }

}
