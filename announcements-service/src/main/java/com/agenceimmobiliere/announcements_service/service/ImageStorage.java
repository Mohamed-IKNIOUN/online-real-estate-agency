package com.agenceimmobiliere.announcements_service.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Stores photo files on disk under app.images.dir (a Docker volume in compose).
 * File names are generated here; client-supplied names are never used.
 */
@Component
public class ImageStorage {

    private final Path root;

    public ImageStorage(@Value("${app.images.dir}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot create image directory " + root, exception);
        }
    }

    public String save(byte[] content, String extension) {
        String name = UUID.randomUUID() + "." + extension;
        try {
            Files.write(resolve(name), content);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot store image", exception);
        }
        return name;
    }

    public Resource load(String name) {
        return new FileSystemResource(resolve(name));
    }

    public void delete(String name) {
        try {
            Files.deleteIfExists(resolve(name));
        } catch (IOException ignored) {
            // A leftover file is harmless; the database row is the source of truth.
        }
    }

    private Path resolve(String name) {
        Path path = root.resolve(name).normalize();
        if (!path.getParent().equals(root)) {
            throw new IllegalArgumentException("Invalid image name");
        }
        return path;
    }
}
