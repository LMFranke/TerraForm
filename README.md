# TerraForm - A 2D Sandbox Game in Java

## About This Project

TerraForm is a 2D sandbox game engine built from scratch in Java with the Swing library. This is a hobby project created to explore core game development concepts, inspired by classics like Terraria and Minecraft.

The repository contains all the source code and assets for this project.

## Core Features

  * **Procedural World Generation:** A horizontally infinite world is generated on the fly using Perlin noise algorithms for terrain and caves.
  * **Chunk System:** The world is loaded and rendered in chunks to maintain performance, allowing for large-scale exploration without significant overhead.
  * **Dynamic 2D Lighting:** A tile-based lighting engine (lightmap) that simulates sunlight and propagates light through open spaces, with seamless transitions across chunk borders.
  * **Player Controller:** Implements basic physics for movement, jumping, and gravity. The player character has animated sprites for idle and walking states in four directions.
  * **Inventory & Hotbar:** A complete inventory and hotbar system allows the player to collect, store, and select items.
  * **World Interaction:** Players can break blocks in the environment to collect them and shape the terrain.

## Technology Stack

  * **Language:** Java
  * **Graphics:** Java Swing
  * **Build Tool:** Maven

## Getting Started

This project is built using Maven. To run it locally, follow these steps:

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/LMFranke/TerraForm.git]
    ```
2.  **Navigate to the project directory:**
    ```bash
    cd TerraForm
    ```
3.  **Compile and run using Maven:**
    ```bash
    mvn compile exec:java -Dexec.mainClass="br.lucasfranke.Main"
    ```

Alternatively, you can import the directory as an existing Maven project into an IDE like IntelliJ IDEA or Eclipse and run the `Main.java` class.
