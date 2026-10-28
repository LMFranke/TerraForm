package br.lucasfranke.world;

import br.lucasfranke.listeners.KeyManager;
import br.lucasfranke.world.light.LightingEngine;

import java.awt.event.KeyEvent;

public class DayNightCycle {

    private static final double TIME_INCREMENT_PER_TICK = 24.0 / (60.0 * 120.0);
    private double timeOfDay = 8.0;

    public void update(LightingEngine lighting, int playerChunkX) {
        if (KeyManager.isKeyPressed(KeyEvent.VK_H) || KeyManager.isKeyPressed(KeyEvent.VK_T)) {
            timeOfDay = (timeOfDay + 1.0) % 24.0;
            KeyManager.unpressKey(KeyEvent.VK_H);
            KeyManager.unpressKey(KeyEvent.VK_T);
        } else {
            timeOfDay = (timeOfDay + TIME_INCREMENT_PER_TICK) % 24.0;
        }

        int targetSunLight = calculateSunLightForHour(timeOfDay);
        lighting.setSunLightLevel(targetSunLight, playerChunkX);
    }

    private int calculateSunLightForHour(double hour) {
        int minLight = LightingEngine.MIN_NIGHT_LIGHT_LEVEL;
        int maxLight = LightingEngine.MAX_LIGHT_LEVEL;

        if (hour >= 7.0 && hour < 17.0) {
            return maxLight;
        } else if (hour >= 17.0 && hour < 20.0) {
            double progress = (hour - 17.0) / 3.0;
            return (int) Math.round(maxLight - progress * (maxLight - minLight));
        } else if (hour >= 4.0 && hour < 7.0) {
            double progress = (hour - 4.0) / 3.0;
            return (int) Math.round(minLight + progress * (maxLight - minLight));
        }
        return minLight;
    }

    public String getFormattedTime() {
        int hours = (int) timeOfDay;
        int minutes = (int) ((timeOfDay - hours) * 60);
        String period = (timeOfDay >= 6.0 && timeOfDay < 18.0) ? "Day" : "Night";
        return String.format("%02d:%02d (%s)", hours, minutes, period);
    }
}