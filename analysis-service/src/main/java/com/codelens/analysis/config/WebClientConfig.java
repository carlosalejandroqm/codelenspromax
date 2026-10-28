package com.codelens.analysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * El contenido de los archivos puede ocupar varios megabytes, asi que se sube el
 * limite de buffer por defecto de WebClient (256 KB).
 */
@Configuration
public class WebClientConfig {

    private static final int MAX_IN_MEMORY_BYTES = 32 * 1024 * 1024;

    @Bean
    WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_BYTES));
    }
}
