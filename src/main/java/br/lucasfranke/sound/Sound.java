package br.lucasfranke.sound;

import java.util.Random;

public enum Sound {

    // Sound effects
    GRASS_BREAK(
            "/sound/grass1.wav",
            "/sound/grass2.wav",
            "/sound/grass3.wav",
            "/sound/grass4.wav"
    ),
    STONE_BREAK(
            "/sound/stone1.wav",
            "/sound/stone2.wav",
            "/sound/stone3.wav",
            "/sound/stone4.wav"
    ),
    PLAYER_JUMP("/sound/pulo.wav"),
    ITEM_PICKUP("/sounds/pegar_item.wav"),

    // Loop sounds
    PLAYER_WALK("/sounds/passos_grama.wav"),

    // Music
    BACKGROUND_MUSIC("/sounds/musica_fundo.wav");

    private final String[] filePaths;
    private static final Random random = new Random();


    /**
     * Enum constructor to associate each sound with its file path.
     *
     * @param filePaths The path to the .wav file from the resources (res) folder.
     */
    Sound(String... filePaths) {
        this.filePaths = filePaths;
    }

    public String[] getFilePaths() {
        return filePaths;
    }

    public String getRandomFilePath() {
        if (filePaths.length == 1) {
            return filePaths[0];
        }
        return filePaths[random.nextInt(filePaths.length)];
    }

}
