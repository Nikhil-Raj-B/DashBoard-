package com.everyday;

/**
 * Standard launcher class to bypass JavaFX runtime checks when launching from a fat jar.
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
