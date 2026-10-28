package com.codelens.analysis.engine;

import com.codelens.analysis.config.AnalysisProperties;
import com.codelens.analysis.engine.model.Advisory;
import com.codelens.analysis.engine.model.ManifestFormat;
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
import java.util.regex.Pattern;

/**
 * Base de datos offline de avisos de dependencias y de formatos de manifiesto.
 *
 * Se mantiene deliberadamente local (sin llamadas a NVD ni a OSV) para que el
 * analisis sea reproducible y funcione sin red. Se puede ampliar dejando
 * {@code advisories.json} y {@code manifests.json} en el directorio externo
 * configurado en {@code codelens.analysis.external-rules-directory}.
 */
@Component
public class AdvisoryDatabase {

    private static final Logger log = LoggerFactory.getLogger(AdvisoryDatabase.class);
    private static final String ADVISORIES = "advisories/advisories.json";
    private static final String MANIFESTS = "advisories/manifests.json";

    private final ObjectMapper objectMapper;
    private final AnalysisProperties properties;

    /** Avisos indexados por ecosistema y nombre de paquete en minusculas. */
    private final Map<String, List<Advisory>> advisoriesByKey = new HashMap<>();
    private final List<CompiledManifestFormat> manifestFormats = new ArrayList<>();

    public AdvisoryDatabase(ObjectMapper objectMapper, AnalysisProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    void load() {
        List<Advisory> advisories = new ArrayList<>(read(ADVISORIES, Advisory.class));
        advisories.addAll(readExternal("advisories.json", Advisory.class));
        for (Advisory advisory : advisories) {
            advisoriesByKey.computeIfAbsent(key(advisory.ecosystem(), advisory.packageName()),
                    k -> new ArrayList<>()).add(advisory);
        }

        List<ManifestFormat> formats = new ArrayList<>(read(MANIFESTS, ManifestFormat.class));
        formats.addAll(readExternal("manifests.json", ManifestFormat.class));
        for (ManifestFormat format : formats) {
            try {
                manifestFormats.add(new CompiledManifestFormat(
                        format,
                        Pattern.compile(format.filePattern(), Pattern.CASE_INSENSITIVE),
                        Pattern.compile(format.entryPattern())));
            } catch (RuntimeException e) {
                log.warn("Formato de manifiesto {} descartado: {}", format.ecosystem(), e.getMessage());
            }
        }

        log.info("Avisos de dependencias cargados: {} paquetes, {} formatos de manifiesto",
                advisoriesByKey.size(), manifestFormats.size());
    }

    private <T> List<T> read(String resource, Class<T> type) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return objectMapper.readerForListOf(type).readValue(in);
        } catch (IOException e) {
            log.error("No se pudo leer {}", resource, e);
            return List.of();
        }
    }

    private <T> List<T> readExternal(String fileName, Class<T> type) {
        String dir = properties.getExternalRulesDirectory();
        if (dir == null || dir.isBlank()) {
            return List.of();
        }
        Path file = Path.of(dir, fileName);
        if (!Files.isReadable(file)) {
            return List.of();
        }
        try (InputStream in = Files.newInputStream(file)) {
            List<T> extra = objectMapper.readerForListOf(type).readValue(in);
            log.info("{} entradas externas cargadas desde {}", extra.size(), file);
            return extra;
        } catch (IOException e) {
            log.warn("Catalogo externo {} ignorado: {}", file, e.getMessage());
            return List.of();
        }
    }

    /** Formatos cuyo patron de ruta coincide con el archivo dado. */
    public List<CompiledManifestFormat> formatsFor(String path) {
        return manifestFormats.stream()
                .filter(format -> format.filePattern().matcher(path).find())
                .toList();
    }

    /** Avisos que afectan a la version indicada del paquete. */
    public List<Advisory> findAffecting(String ecosystem, String packageName, String version) {
        List<Advisory> candidates = advisoriesByKey.get(key(ecosystem, packageName));
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        String normalized = VersionRange.normalize(version);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return candidates.stream()
                .filter(advisory -> VersionRange.parse(advisory.vulnerableRange()).contains(normalized))
                .toList();
    }

    public int advisoryCount() {
        return advisoriesByKey.values().stream().mapToInt(List::size).sum();
    }

    public List<ManifestFormat> manifestFormats() {
        return manifestFormats.stream().map(CompiledManifestFormat::format).toList();
    }

    private String key(String ecosystem, String packageName) {
        return (ecosystem + "::" + packageName).toLowerCase(Locale.ROOT);
    }

    /** Formato de manifiesto con sus expresiones ya compiladas. */
    public record CompiledManifestFormat(ManifestFormat format, Pattern filePattern, Pattern entryPattern) {
    }
}
