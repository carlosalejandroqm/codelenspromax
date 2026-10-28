package com.codelens.user.service;

import com.codelens.user.dto.AuthResponse;
import com.codelens.user.dto.LoginRequest;
import com.codelens.user.dto.RegisterRequest;
import com.codelens.user.dto.UserDto;
import com.codelens.user.model.User;
import com.codelens.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Registro, login y consulta de cuentas. */
@Service
public class UserAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserAccountService.class);

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserAccountService(UserRepository repository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new UserAlreadyExistsException("El usuario '" + username + "' ya esta registrado");
        }
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new UserAlreadyExistsException("El correo " + email + " ya esta registrado");
        }

        User user = new User(UUID.randomUUID().toString(), username, email,
                request.fullName().trim(), passwordEncoder.encode(request.password()));
        user.markLogin(Instant.now());
        repository.save(user);
        log.info("Usuario {} registrado", username);
        return authenticated(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.username().trim();
        User user = (identifier.contains("@")
                ? repository.findByEmailIgnoreCase(identifier)
                : repository.findByUsernameIgnoreCase(identifier))
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o contrasena incorrectos"));

        user.markLogin(Instant.now());
        return authenticated(user);
    }

    @Transactional(readOnly = true)
    public UserDto get(String id) {
        return repository.findById(id)
                .map(UserDto::from)
                .orElseThrow(() -> new UserNotFoundException("No existe el usuario " + id));
    }

    private AuthResponse authenticated(User user) {
        JwtService.IssuedToken issued = jwtService.issue(user);
        return new AuthResponse(issued.token(), "Bearer", issued.expiresAt(), UserDto.from(user));
    }
}
