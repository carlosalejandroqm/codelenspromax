package com.codelens.source.service;

import com.codelens.source.dto.FileContentDto;
import com.codelens.source.dto.GitImportRequest;
import com.codelens.source.model.*;
import com.codelens.source.repository.SourceFileRepository;
import com.codelens.source.repository.SourceSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Orquesta el ciclo completo de obtencion de codigo: fetch -> inventario -> READY. */
@Service
public class SourceIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SourceIngestionService.class);

    private final SourceSnapshotRepository snapshotRepository;
    private final SourceFileRepository fileRepository;
    private final WorkspaceManager workspaceManager;
    private final GitRepositoryFetcher gitFetcher;
    private final ZipArchiveExtractor zipExtractor;
    private final FileInventoryScanner scanner;

    public SourceIngestionService(SourceSnapshotRepository snapshotRepository,
                                 SourceFileRepository fileRepository,
                                 WorkspaceManager workspaceManager,
                                 GitRepositoryFetcher gitFetcher,
                                 ZipArchiveExtractor zipExtractor,
                                 FileInventoryScanner scanner) {
        this.snapshotRepository = snapshotRepository;
        this.fileRepository = fileRepository;
        this.workspaceManager = workspaceManager;
        this.gitFetcher = gitFetcher;
        this.zipExtractor = zipExtractor;
        this.scanner = scanner;
    }

    @Transactional
    public SourceSnapshot importFromGit(GitImportRequest request, String ownerId) {
        String id = UUID.randomUUID().toString();
        SourceSnapshot snapshot = new SourceSnapshot(id, SourceType.GIT,
                gitFetcher.projectNameFrom(request.repositoryUrl()));
        snapshot.setOwnerId(ownerId);
        snapshot.setRepositoryUrl(request.repositoryUrl());
        snapshot.setBranch(request.branch());
        snapshot.setStatus(SnapshotStatus.FETCHING);

        Path workspace = workspaceManager.createWorkspace(id);
        snapshot.setWorkspacePath(workspace.toString());

        try {
            String commit = gitFetcher.clone(request.repositoryUrl(), request.branch(),
                    request.accessToken(), workspace);
            snapshot.setCommitId(commit);
            scanner.scanInto(snapshot, workspace);
            snapshot.markReady(Instant.now());
            log.info("Snapshot git {} listo: {} archivos analizables de {}",
                    id, snapshot.getAnalyzableFiles(), snapshot.getTotalFiles());
        } catch (RuntimeException e) {
            workspaceManager.delete(workspace);
            snapshot.markFailed(e.getMessage());
            snapshotRepository.save(snapshot);
            throw e;
        }

        return snapshotRepository.save(snapshot);
    }

    @Transactional
    public SourceSnapshot importFromZip(MultipartFile archive, String ownerId) {
        if (archive == null || archive.isEmpty()) {
            throw new SourceIngestionException("Debes adjuntar un archivo zip con el codigo");
        }
        String original = archive.getOriginalFilename() == null ? "codigo.zip" : archive.getOriginalFilename();
        if (!original.toLowerCase().endsWith(".zip")) {
            throw new SourceIngestionException("Solo se aceptan archivos .zip");
        }
        return importArchive(archive, SourceType.ZIP, original.replaceAll("(?i)\\.zip$", ""), ownerId);
    }

    /**
     * Carpeta local: el navegador la empaqueta como zip (sin node_modules, .git,
     * etc.) para no chocar con el limite de partes multipart del servidor.
     */
    @Transactional
    public SourceSnapshot importFromFolder(MultipartFile archive, String folderName, String ownerId) {
        if (archive == null || archive.isEmpty()) {
            throw new SourceIngestionException("La carpeta seleccionada no contiene archivos");
        }
        String name = folderName == null || folderName.isBlank() ? "carpeta-local" : folderName.trim();
        return importArchive(archive, SourceType.FOLDER, name, ownerId);
    }

    private SourceSnapshot importArchive(MultipartFile archive, SourceType type, String projectName, String ownerId) {
        String id = UUID.randomUUID().toString();
        SourceSnapshot snapshot = new SourceSnapshot(id, type, projectName);
        snapshot.setOwnerId(ownerId);
        snapshot.setStatus(SnapshotStatus.FETCHING);

        Path workspace = workspaceManager.createWorkspace(id);

        try (InputStream in = archive.getInputStream()) {
            zipExtractor.extract(in, workspace);
            Path projectRoot = zipExtractor.stripSingleRootDirectory(workspace);
            snapshot.setWorkspacePath(projectRoot.toString());
            scanner.scanInto(snapshot, projectRoot);
            snapshot.markReady(Instant.now());
            log.info("Snapshot {} {} listo: {} archivos analizables de {}",
                    type, id, snapshot.getAnalyzableFiles(), snapshot.getTotalFiles());
        } catch (IOException e) {
            workspaceManager.delete(workspace);
            snapshot.markFailed("No se pudo leer el archivo subido: " + e.getMessage());
            snapshotRepository.save(snapshot);
            throw new SourceIngestionException("No se pudo leer el archivo subido", e);
        } catch (RuntimeException e) {
            workspaceManager.delete(workspace);
            snapshot.markFailed(e.getMessage());
            snapshotRepository.save(snapshot);
            throw e;
        }

        return snapshotRepository.save(snapshot);
    }

    @Transactional(readOnly = true)
    public SourceSnapshot get(String id) {
        return snapshotRepository.findById(id)
                .orElseThrow(() -> new SourceNotFoundException("No existe el snapshot " + id));
    }

    /** Con ownerId solo devuelve los snapshots de ese usuario. */
    @Transactional(readOnly = true)
    public List<SourceSnapshot> list(String ownerId) {
        return ownerId == null || ownerId.isBlank()
                ? snapshotRepository.findAllByOrderByCreatedAtDesc()
                : snapshotRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
    }

    @Transactional(readOnly = true)
    public List<SourceFile> listFiles(String snapshotId, boolean onlyAnalyzable) {
        get(snapshotId);
        return onlyAnalyzable
                ? fileRepository.findBySnapshotIdAndAnalyzableTrueOrderByPathAsc(snapshotId)
                : fileRepository.findBySnapshotIdOrderByPathAsc(snapshotId);
    }

    /** Lee el contenido de un archivo del snapshot desde el workspace. */
    @Transactional(readOnly = true)
    public FileContentDto readFile(String snapshotId, String path) {
        SourceSnapshot snapshot = get(snapshotId);
        SourceFile file = fileRepository.findBySnapshotIdAndPath(snapshotId, path)
                .orElseThrow(() -> new SourceNotFoundException("El snapshot no contiene " + path));
        if (!file.isAnalyzable()) {
            throw new SourceIngestionException("El archivo no es texto analizable: " + file.getSkipReason());
        }

        Path root = Path.of(snapshot.getWorkspacePath());
        Path resolved = workspaceManager.resolveInside(root, path);
        try {
            String content = Files.readString(resolved, StandardCharsets.UTF_8);
            return new FileContentDto(snapshotId, path, file.getLanguage(), file.getLineCount(), content);
        } catch (IOException e) {
            throw new SourceIngestionException("No se pudo leer " + path + ": " + e.getMessage(), e);
        }
    }

    /** Borra el snapshot y su workspace. Idempotente. */
    @Transactional
    public void purge(String id) {
        SourceSnapshot snapshot = get(id);
        if (snapshot.getWorkspacePath() != null) {
            workspaceManager.delete(workspaceManager.root().resolve(id));
        }
        snapshotRepository.delete(snapshot);
        log.info("Snapshot {} purgado", id);
    }
}
