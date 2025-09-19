package br.lucasfranke.sound;

import javax.sound.sampled.*;
import java.net.URL;
import java.util.*;

public class SoundManager {

    private final Map<Sound, List<Clip>> soundClips = new EnumMap<>(Sound.class);
    private final Map<Sound, List<FloatControl>> gainControls = new EnumMap<>(Sound.class);
    private static final Random random = new Random();

    public SoundManager() {
        loadAllSounds();
        setMasterVolume(0.5f);
    }

    /**
     * Loads all sounds defined in the Sound enum into memory.
     * This method must be called once at the start of the game.
     */
    public void loadAllSounds() {
        for (Sound sound : Sound.values()) {
            List<Clip> clips = new ArrayList<>();
            List<FloatControl> controls = new ArrayList<>();

            for (String filePath : sound.getFilePaths()) {
                try {
                    URL url = getClass().getResource(filePath);
                    if (url == null) {
                        System.err.println("Could not find sound file: " + filePath);
                        continue;
                    }

                    AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioStream);
                    clips.add(clip);

                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    controls.add(gainControl);

                } catch (Exception e) {
                    System.err.println("Error while trying to load the sound's variant: " + filePath);
                    e.printStackTrace();
                }
            }
            soundClips.put(sound, clips);
            gainControls.put(sound, controls);
        }
    }

    public void playSound(Sound sound) {
        List<Clip> clips = soundClips.get(sound);
        if (clips == null || clips.isEmpty()) {
            return;
        }

        Clip clipToPlay = clips.get(random.nextInt(clips.size()));

        if (clipToPlay != null) {
            if (clipToPlay.isRunning()) {
                clipToPlay.stop();
            }
            clipToPlay.setFramePosition(0);
            clipToPlay.start();
        }
    }

    public void loopSound(Sound sound) {
        List<Clip> clips = soundClips.get(sound);
        if (clips == null || clips.isEmpty()) {
            return;
        }
        Clip clip = clips.get(0);
        if (clip != null && !clip.isRunning()) {
            clip.setFramePosition(0);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void stopSound(Sound sound) {
        List<Clip> clips = soundClips.get(sound);
        if (clips != null) {
            for (Clip clip : clips) {
                if (clip.isRunning()) {
                    clip.stop();
                }
            }
        }
    }

    public void setMasterVolume(float volume) {
        float clampedVolume = Math.max(0.0f, Math.min(1.0f, volume));

        for (List<FloatControl> controls : gainControls.values()) {
            for (FloatControl control : controls) {
                float min = control.getMinimum();
                float max = control.getMaximum();
                float range = max - min;
                float db = (range * clampedVolume) + min;
                control.setValue(db);
            }
        }
    }
}
