package com.clothingstore.inventory.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Copies a user-picked image file into a local "images" folder next to the
 * database, so the app doesn't depend on the original file staying in place
 * (e.g. if the user picked something from their Downloads folder and later
 * deletes it). Returns a relative path that's safe to store in the DB and
 * safe to move the whole app folder around with.
 */
public final class ImageStorage {

    private static final Path IMAGES_DIR = Path.of("images");

    private ImageStorage() {}

    /** Copies sourceFile into images/, returns the relative path to store on the Item. */
    public static String store(Path sourceFile) {
        try {
            Files.createDirectories(IMAGES_DIR);

            String original = sourceFile.getFileName().toString();
            String extension = original.contains(".")
                    ? original.substring(original.lastIndexOf('.'))
                    : "";
            String storedName = UUID.randomUUID() + extension;

            Path destination = IMAGES_DIR.resolve(storedName);
            Files.copy(sourceFile, destination, StandardCopyOption.REPLACE_EXISTING);

            return destination.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store image", e);
        }
    }

    /** Resolves a stored relative path back to an absolute file path for display. */
    public static Path resolve(String relativePath) {
        return Path.of(relativePath);
    }
}
