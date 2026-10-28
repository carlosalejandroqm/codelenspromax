package com.codelens.source.service;

import com.codelens.source.config.StorageProperties;
import com.codelens.source.model.SourceFile;
import com.codelens.source.model.SourceSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Recorre el workspace y construye el inventario de archivos del snapshot,
 * decidiendo cuales son analizables.
 *
 * La deteccion de binarios no depende de la extension unicamente: tambien
 * inspecciona los primeros bytes buscando NUL, de modo que archivos de
 * lenguajes desconocidos sigan entrando al analisis si son texto.
 */
@Component
public class FileInventoryScanner {

    private static final Logger log = LoggerFactory.getLogger(FileInventoryScanner.class);
    private static final int SNIFF_BYTES = 8192;

    private final StorageProperties properties;
    private final LanguageDetector languageDetector;

    public FileInventoryScanner(StorageProperties properties, LanguageDetector languageDetector) {
        this.properties = properties;
        this.languageDetector = languageDetector;
    }

    public void scanInto(SourceSnapshot snapshot, Path root) {
        Counters counters = new Counters();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(root)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    if (properties.getExcludedDirectories().contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (counters.files >= properties.getMaxFiles()) {
                        log.warn("Limite de {} archivos alcanzado en {}", properties.getMaxFiles(), root);
                        return FileVisitResult.TERMINATE;
                    }
                    if (!attrs.isRegularFile()) {
                        return FileVisitResult.CONTINUE;
                    }
                    snapshot.addFile(describe(root, file, attrs, counters));
                    counters.files++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.debug("Archivo ilegible {}: {}", file, exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new SourceIngestionException("Fallo al inventariar el codigo: " + e.getMessage(), e);
        }

        snapshot.setTotalFiles(counters.files);
        snapshot.setAnalyzableFiles(counters.analyzable);
        snapshot.setSkippedFiles(counters.files - counters.analyzable);
        snapshot.setTotalBytes(counters.bytes);
        snapshot.setTotalLines(counters.lines);
    }

    private SourceFile describe(Path root, Path file, BasicFileAttributes attrs, Counters counters) {
        String relative = root.relativize(file).toString().replace('\\', '/');
        String fileName = file.getFileName().toString();
        String extension = languageDetector.extensionOf(fileName);
        String language = languageDetector.detect(fileName, extension);
        long size = attrs.size();
        counters.bytes += size;

        String skipReason = skipReason(extension, size, file);
        if (skipReason != null) {
            return new SourceFile(relative, extension, language, size, 0, false, skipReason, null);
        }

        try {
            byte[] bytes = Files.readAllBytes(file);
            int lines = countLines(bytes);
            counters.analyzable++;
            counters.lines += lines;
            return new SourceFile(relative, extension, language, size, lines, true, null, sha256(bytes));
        } catch (IOException e) {
            return new SourceFile(relative, extension, language, size, 0, false, "UNREADABLE", null);
        }
    }

    /** @return motivo de exclusion, o null si el archivo es analizable. */
    private String skipReason(String extension, long size, Path file) {
        if (size == 0) {
            return "EMPTY";
        }
        if (size > properties.getMaxFileSizeBytes()) {
            return "TOO_LARGE";
        }
        if (properties.getBinaryExtensions().contains(extension)) {
            return "BINARY_EXTENSION";
        }
        if (looksBinary(file)) {
            return "BINARY_CONTENT";
        }
        return null;
    }

    /** Heuristica de binario: un byte NUL en la cabecera del archivo. */
    private boolean looksBinary(Path file) {
        try (var in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(SNIFF_BYTES);
            for (byte b : head) {
                if (b == 0) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private int countLines(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.isEmpty()) {
            return 0;
        }
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') {
                lines++;
            }
        }
        return lines;
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private static final class Counters {
        int files;
        int analyzable;
        long bytes;
        long lines;
    }
}
