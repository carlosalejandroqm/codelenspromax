package com.codelens.user.controller;

import com.codelens.user.dto.UserDto;
import com.codelens.user.service.UserAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints protegidos: el gateway ya valido el token y envia el usuario en X-User-Id. */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserAccountService service;

    public UserController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public UserDto me(@RequestHeader("X-User-Id") String userId) {
        return service.get(userId);
    }
}
