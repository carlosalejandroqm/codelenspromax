package com.codelens.source.service;

import com.codelens.source.config.StorageProperties;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

/**
 * Clona repositorios remotos con JGit (clone superficial, depth=1) porque el
 * analisis solo necesita el arbol de trabajo, no el historial completo.
 */
@Component
public class GitRepositoryFetcher {

    private static final Logger log = LoggerFactory.getLogger(GitRepositoryFetcher.class);

    private final StorageProperties properties;

    public GitRepositoryFetcher(StorageProperties properties) {
        this.properties = properties;
    }

    /**
     * @return id del commit clonado (HEAD), o null si no se pudo resolver.
     */
    public String clone(String repositoryUrl, String branch, String accessToken, Path target) {
        validateUrl(repositoryUrl);

        CloneCommand command = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(target.toFile())
                .setDepth(1)
                .setCloneSubmodules(false)
                .setNoTags();

        if (branch != null && !branch.isBlank()) {
            command.setBranch(branch.trim()).setBranchesToClone(java.util.List.of("refs/heads/" + branch.trim()));
        }
        if (accessToken != null && !accessToken.isBlank()) {
            // GitHub/GitLab aceptan el token como password con cualquier usuario.
            command.setCredentialsProvider(new UsernamePasswordCredentialsProvider(accessToken.trim(), ""));
        }

        try (Git git = command.call()) {
            ObjectId head = git.getRepository().resolve("HEAD");
            return head != null ? head.getName() : null;
        } catch (Exception e) {
            log.warn("Fallo el clonado de {}: {}", repositoryUrl, e.getMessage());
            throw new SourceIngestionException(
                    "No se pudo clonar el repositorio. Verifica la URL, la rama y si requiere token: "
                            + e.getMessage(), e);
        }
    }

    /** Rechaza esquemas no http(s) y hosts no permitidos si hay lista blanca. */
    private void validateUrl(String repositoryUrl) {
        URI uri;
        try {
            uri = new URI(repositoryUrl);
        } catch (URISyntaxException e) {
            throw new SourceIngestionException("URL de repositorio invalida: " + repositoryUrl);
        }
        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equals("http") || scheme.equals("https"))) {
            throw new SourceIngestionException("Solo se aceptan repositorios http/https");
        }
        var allowed = properties.getAllowedGitHosts();
        if (!allowed.isEmpty() && (uri.getHost() == null || !allowed.contains(uri.getHost()))) {
            throw new SourceIngestionException("Host git no permitido: " + uri.getHost());
        }
    }

    /** Deriva un nombre de proyecto legible desde la URL del repo. */
    public String projectNameFrom(String repositoryUrl) {
        String cleaned = repositoryUrl.replaceAll("\\.git$", "").replaceAll("/+$", "");
        int slash = cleaned.lastIndexOf('/');
        String name = slash >= 0 ? cleaned.substring(slash + 1) : cleaned;
        return name.isBlank() ? "repositorio" : name;
    }
}
