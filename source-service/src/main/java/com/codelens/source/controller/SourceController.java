package com.codelens.source.controller;

import com.codelens.source.dto.*;
import com.codelens.source.model.SourceFile;
import com.codelens.source.model.SourceSnapshot;
import com.codelens.source.service.SourceIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** API del microservicio de obtencion de codigo fuente. */
@RestController
@RequestMapping("/api/v1/sources")
public class SourceController {

    /** Identidad que agrega el gateway tras validar el token. */
    private static final String USER_HEADER = "X-User-Id";

    private final SourceIngestionService service;

    public SourceController(SourceIngestionService service) {
        this.service = service;
    }

    /** Importa un repositorio remoto por URL. */
    @PostMapping("/git")
    public ResponseEntity<SourceSnapshotDto> importGit(@Valid @RequestBody GitImportRequest request,
                                                       @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        SourceSnapshot snapshot = service.importFromGit(request, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(SourceSnapshotDto.from(snapshot));
    }

    /** Importa un zip subido desde el frontend. */
    @PostMapping(path = "/zip", consumes = "multipart/form-data")
    public ResponseEntity<SourceSnapshotDto> importZip(@RequestParam("file") MultipartFile file,
                                                       @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        SourceSnapshot snapshot = service.importFromZip(file, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(SourceSnapshotDto.from(snapshot));
    }

    /** Importa una carpeta local que el navegador empaqueto como zip. */
    @PostMapping(path = "/folder", consumes = "multipart/form-data")
    public ResponseEntity<SourceSnapshotDto> importFolder(@RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "name", required = false) String name,
                                                          @RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        SourceSnapshot snapshot = service.importFromFolder(file, name, ownerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(SourceSnapshotDto.from(snapshot));
    }

    @GetMapping
    public List<SourceSnapshotDto> list(@RequestHeader(value = USER_HEADER, required = false) String ownerId) {
        return service.list(ownerId).stream().map(SourceSnapshotDto::from).toList();
    }

    @GetMapping("/{id}")
    public SourceSnapshotDto get(@PathVariable String id) {
        return SourceSnapshotDto.from(service.get(id));
    }

    /** Inventario de archivos; por defecto solo los analizables. */
    @GetMapping("/{id}/files")
    public List<SourceFileDto> files(@PathVariable String id,
                                     @RequestParam(defaultValue = "true") boolean onlyAnalyzable) {
        List<SourceFile> files = service.listFiles(id, onlyAnalyzable);
        return files.stream().map(SourceFileDto::from).toList();
    }

    /** Contenido de un archivo concreto del snapshot. */
    @GetMapping("/{id}/content")
    public FileContentDto content(@PathVariable String id, @RequestParam String path) {
        return service.readFile(id, path);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> purge(@PathVariable String id) {
        service.purge(id);
        return ResponseEntity.noContent().build();
    }
}
