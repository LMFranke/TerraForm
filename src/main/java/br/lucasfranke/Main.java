package br.lucasfranke;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame("2D");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        try {
            Assets.init();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        EnginePanel panel = new EnginePanel();
        window.add(panel);
        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);

        panel.startEngine();
    }
}