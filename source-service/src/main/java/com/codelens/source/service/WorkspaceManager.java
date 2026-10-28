package com.codelens.source.service;

import com.codelens.source.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

/** Crea, resuelve y elimina los directorios de trabajo de cada snapshot. */
@Component
public class WorkspaceManager {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    private final StorageProperties properties;

    public WorkspaceManager(StorageProperties properties) {
        this.properties = properties;
    }

    public Path createWorkspace(String snapshotId) {
        try {
            Path dir = root().resolve(snapshotId);
            Files.createDirectories(dir);
            return dir;
        } catch (IOException e) {
            throw new SourceIngestionException("No se pudo crear el workspace: " + e.getMessage(), e);
        }
    }

    public Path root() {
        return Paths.get(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
    }

    /**
     * Resuelve una ruta relativa dentro del workspace rechazando cualquier
     * intento de salir de el (path traversal).
     */
    public Path resolveInside(Path workspace, String relativePath) {
        Path base = workspace.toAbsolutePath().normalize();
        Path target = base.resolve(relativePath).normalize();
        if (!target.startsWith(base)) {
            throw new SourceIngestionException("Ruta fuera del workspace: " + relativePath);
        }
        return target;
    }

    public void delete(Path workspace) {
        if (workspace == null || !Files.exists(workspace)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(workspace)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    log.warn("No se pudo borrar {}: {}", p, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("Fallo al limpiar el workspace {}: {}", workspace, e.getMessage());
        }
    }
}
