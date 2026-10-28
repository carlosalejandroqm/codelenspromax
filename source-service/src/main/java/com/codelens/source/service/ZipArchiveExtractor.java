package com.codelens.source.service;

import com.codelens.source.config.StorageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Descomprime un zip en el workspace con dos protecciones explicitas:
 * <ul>
 *   <li>zip-slip: cada entrada se resuelve y se verifica que quede dentro del destino.</li>
 *   <li>zip-bomb: se aborta si el total descomprimido supera el limite configurado.</li>
 * </ul>
 */
@Component
public class ZipArchiveExtractor {

    private static final Logger log = LoggerFactory.getLogger(ZipArchiveExtractor.class);
    private static final int BUFFER = 8192;

    private final StorageProperties properties;
    private final WorkspaceManager workspaceManager;

    public ZipArchiveExtractor(StorageProperties properties, WorkspaceManager workspaceManager) {
        this.properties = properties;
        this.workspaceManager = workspaceManager;
    }

    public void extract(InputStream source, Path target) {
        long written = 0;
        int entries = 0;

        try (ZipInputStream zip = new ZipInputStream(source)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > properties.getMaxFiles()) {
                    throw new SourceIngestionException(
                            "El zip contiene mas de " + properties.getMaxFiles() + " entradas");
                }

                String name = entry.getName().replace('\\', '/');
                if (name.isBlank() || name.startsWith("__MACOSX/") || name.contains("/__MACOSX/")) {
                    zip.closeEntry();
                    continue;
                }

                Path resolved = workspaceManager.resolveInside(target, name);
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                    zip.closeEntry();
                    continue;
                }

                Files.createDirectories(resolved.getParent());
                written += copy(zip, resolved, written);
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new SourceIngestionException("El archivo no es un zip valido: " + e.getMessage(), e);
        }

        if (entries == 0) {
            throw new SourceIngestionException("El zip esta vacio");
        }
        log.debug("Extraidas {} entradas ({} bytes) en {}", entries, written, target);
    }

    private long copy(ZipInputStream zip, Path destination, long alreadyWritten) throws IOException {
        long total = 0;
        byte[] buffer = new byte[BUFFER];
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(destination))) {
            int read;
            while ((read = zip.read(buffer)) > 0) {
                total += read;
                if (alreadyWritten + total > properties.getMaxSnapshotBytes()) {
                    throw new SourceIngestionException(
                            "El contenido descomprimido supera el limite de "
                                    + properties.getMaxSnapshotBytes() + " bytes");
                }
                out.write(buffer, 0, read);
            }
        }
        return total;
    }

    /**
     * Si el zip contiene un unico directorio raiz (caso tipico de GitHub),
     * devuelve ese directorio para que las rutas del inventario no lo incluyan.
     */
    public Path stripSingleRootDirectory(Path extracted) {
        try (var children = Files.list(extracted)) {
            var list = children.toList();
            if (list.size() == 1 && Files.isDirectory(list.get(0))) {
                return list.get(0);
            }
        } catch (IOException e) {
            log.debug("No se pudo inspeccionar la raiz de {}: {}", extracted, e.getMessage());
        }
        return extracted;
    }
}
