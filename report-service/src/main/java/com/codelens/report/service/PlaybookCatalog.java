package com.codelens.report.service;

import com.codelens.report.config.ReportProperties;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Carga y resuelve los playbooks de remediacion. */
@Component
public class PlaybookCatalog {

    private static final Logger log = LoggerFactory.getLogger(PlaybookCatalog.class);
    private static final String BUILTIN = "playbooks/playbooks.json";
    private static final String EXTERNAL = "playbooks.json";

    private final ObjectMapper objectMapper;
    private final ReportProperties properties;
    private final List<Compiled> playbooks = new ArrayList<>();

    public PlaybookCatalog(ObjectMapper objectMapper, ReportProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    @PostConstruct
    void load() {
        List<Playbook> definitions = new ArrayList<>(read(BUILTIN));
        definitions.addAll(readExternal());

        for (Playbook playbook : definitions) {
            try {
                playbooks.add(new Compiled(playbook,
                        Pattern.compile(playbook.ruleMatch(), Pattern.CASE_INSENSITIVE)));
            } catch (PatternSyntaxException e) {
                log.warn("Playbook descartado ({}): {}", playbook.ruleMatch(), e.getDescription());
            }
        }
        log.info("Playbooks de remediacion cargados: {}", playbooks.size());
    }

    private List<Playbook> read(String resource) {
        try (InputStream in = new ClassPathResource(resource).getInputStream()) {
            return objectMapper.readerForListOf(Playbook.class).readValue(in);
        } catch (IOException e) {
            log.error("No se pudo leer {}", resource, e);
            return List.of();
        }
    }

    /** Los playbooks externos se evaluan despues, asi que ganan los internos. */
    private List<Playbook> readExternal() {
        String dir = properties.getExternalPlaybooksDirectory();
        if (dir == null || dir.isBlank()) {
            return List.of();
        }
        Path file = Path.of(dir, EXTERNAL);
        if (!Files.isReadable(file)) {
            return List.of();
        }
        try (InputStream in = Files.newInputStream(file)) {
            List<Playbook> extra = objectMapper.readerForListOf(Playbook.class).readValue(in);
            log.info("Playbooks externos cargados desde {}: {}", file, extra.size());
            return extra;
        } catch (IOException e) {
            log.warn("Playbooks externos ignorados ({}): {}", file, e.getMessage());
            return List.of();
        }
    }

    /** Primer playbook cuyo patron encaja con el id de la regla. */
    public Optional<Playbook> forRule(String ruleId) {
        if (ruleId == null) {
            return Optional.empty();
        }
        return playbooks.stream()
                .filter(compiled -> compiled.pattern().matcher(ruleId).find())
                .map(Compiled::playbook)
                .findFirst();
    }

    public int size() {
        return playbooks.size();
    }

    private record Compiled(Playbook playbook, Pattern pattern) {
    }
}
