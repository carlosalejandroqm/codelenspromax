package com.codelens.analysis.engine;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.model.LanguageProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registro de perfiles de lenguaje cargados desde JSON.
 *
 * Es el punto que hace abstracto al analizador: resuelve cualquier archivo a un
 * perfil y, cuando la extension no esta registrada, devuelve el perfil
 * {@code generic}, de modo que el analisis nunca se salta un archivo de texto
 * por ser de un lenguaje desconocido.
 */
@Component
public class LanguageRegistry {

    private static final Logger log = LoggerFactory.getLogger(LanguageRegistry.class);
    private static final String BUILTIN = "languages/languages.json";
    private static final String EXTERNAL = "languages.json";

    private final ObjectMapper objectMapper;
    private final AnalysisProperties properties;

    private final Map<String, LanguageProfile> byId = new LinkedHashMap<>();
    private final Map<String, LanguageProfile> byExtension = new ConcurrentHashMap<>();
    private final Map<String, LanguageProfile> byFileName = new ConcurrentHashMap<>();
    private LanguageProfile genericProfile;

    public LanguageRegistry(ObjectMapper objectMapper, AnalysisProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    void load() {
        List<LanguageProfile> profiles = new ArrayList<>(readBuiltin());
        profiles.addAll(readExternal());

        for (LanguageProfile profile : profiles) {
            byId.put(profile.id(), profile);
            profile.extensions().forEach(ext -> byExtension.put(ext.toLowerCase(Locale.ROOT), profile));
            profile.fileNames().forEach(name -> byFileName.put(name.toLowerCase(Locale.ROOT), profile));
            if (profile.generic()) {
                genericProfile = profile;
            }
        }

        if (genericProfile == null) {
            genericProfile = fallbackGeneric();
            byId.put(genericProfile.id(), genericProfile);
        }

        log.info("Perfiles de lenguaje cargados: {} ({} extensiones mapeadas)", byId.size(), byExtension.size());
    }

    private List<LanguageProfile> readBuiltin() {
        try (InputStream in = new ClassPathResource(BUILTIN).getInputStream()) {
            return objectMapper.readerForListOf(LanguageProfile.class).readValue(in);
        } catch (IOException e) {
            log.error("No se pudo leer {}; se usara solo el perfil generico", BUILTIN, e);
            return List.of();
        }
    }

    /** Perfiles adicionales aportados por el operador sin recompilar. */
    private List<LanguageProfile> readExternal() {
        String dir = properties.getExternalRulesDirectory();
        if (dir == null || dir.isBlank()) {
            return List.of();
        }
        Path file = Path.of(dir, EXTERNAL);
        if (!Files.isReadable(file)) {
            return List.of();
        }
        try (InputStream in = Files.newInputStream(file)) {
            List<LanguageProfile> extra = objectMapper.readerForListOf(LanguageProfile.class).readValue(in);
            log.info("Perfiles externos cargados desde {}: {}", file, extra.size());
            return extra;
        } catch (IOException e) {
            log.warn("Perfiles externos ignorados ({}): {}", file, e.getMessage());
            return List.of();
        }
    }

    /**
     * Resuelve el perfil de un archivo por nombre exacto y luego por extension.
     *
     * @return siempre un perfil; el generico cuando no hay coincidencia.
     */
    public LanguageProfile resolve(String path) {
        String fileName = fileNameOf(path).toLowerCase(Locale.ROOT);

        LanguageProfile byName = byFileName.get(fileName);
        if (byName != null) {
            return byName;
        }
        if (fileName.startsWith(".env")) {
            LanguageProfile dotenv = byId.get("dotenv");
            if (dotenv != null) {
                return dotenv;
            }
        }

        // Extension compuesta primero (tsx antes que x, tar.gz antes que gz).
        int firstDot = fileName.indexOf('.');
        if (firstDot >= 0 && firstDot < fileName.length() - 1) {
            LanguageProfile compound = byExtension.get(fileName.substring(firstDot + 1));
            if (compound != null) {
                return compound;
            }
        }
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fileName.length() - 1) {
            LanguageProfile simple = byExtension.get(fileName.substring(lastDot + 1));
            if (simple != null) {
                return simple;
            }
        }
        return genericProfile;
    }

    public Optional<LanguageProfile> byId(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<LanguageProfile> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public LanguageProfile generic() {
        return genericProfile;
    }

    private String fileNameOf(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    /** Perfil de reserva si el JSON no define uno generico. */
    private LanguageProfile fallbackGeneric() {
        return new LanguageProfile(
                "generic", "Other", Set.of(), Set.of(),
                List.of("//", "#", "--", ";"),
                List.of(new LanguageProfile.BlockComment("/*", "*/")),
                List.of("\"", "'"),
                LanguageProfile.BlockStyle.BRACES,
                Set.of("if", "else", "for", "while", "case", "catch", "switch", "when", "and", "or"),
                List.of("^\\s*(public|private|protected|static|func|function|def|fn|sub|proc)\\b"),
                List.of("(^|/)(test|tests|spec|specs)/", "[._-](test|spec)\\.[a-z0-9]+$"),
                Set.of(),
                true);
    }
}
