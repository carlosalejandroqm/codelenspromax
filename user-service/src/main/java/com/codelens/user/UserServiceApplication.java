package com.codelens.user;

import com.codelens.user.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Microservicio 4 - Usuarios.
 *
 * Responsabilidad unica: registrar cuentas, verificar credenciales y emitir el
 * token JWT que el gateway valida en cada peticion. Los demas servicios nunca
 * ven el token; solo reciben el usuario ya autenticado en cabeceras.
 */
@SpringBootApplication
@EnableConfigurationProperties(SecurityProperties.class)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
