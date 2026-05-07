package ru.job4j.auth.controller;

import ru.job4j.auth.dto.response.UserResponse;
import ru.job4j.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final AuthService authService;

    @GetMapping("/validate")
    public UserResponse validateToken(Authentication authentication) {
        String username = authentication.getName();
        return authService.getCurrentUser(username);
    }
}
