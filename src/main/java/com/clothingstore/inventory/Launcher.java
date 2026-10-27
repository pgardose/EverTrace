package com.clothingstore.inventory;

/**
 * Entry point for running outside "mvn javafx:run" (NetBeans Run button, the
 * shaded jar, jpackage). A main class that itself extends javafx.application.Application
 * makes the JVM demand JavaFX on the module path and fails with "JavaFX runtime
 * components are missing". Starting from this plain class avoids that check.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
