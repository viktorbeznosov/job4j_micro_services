package ru.job4j.payment.security;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import ru.job4j.payment.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient restClient;

    @Retry(name="authService")
    @CircuitBreaker(name = "authService", fallbackMethod = "validateTokenFallback")
    @TimeLimiter(name = "authService")
    public CurrentUserDto validateTokenAndGetUser(String authorizationHeader) {
        try {
            UserResponse userResponse = restClient.get()
                .uri("/api/auth/internal/validate")
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(UserResponse.class);
            
            if (userResponse == null) {
                throw new RuntimeException("Auth service returned null user");
            }
            
            return mapToCurrentUserDto(userResponse);
        } catch (Exception e) {
            log.error("Failed to validate token with auth service: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired token", e);
        }
    }

    @Retry(name="authService")
    @CircuitBreaker(name = "authService", fallbackMethod = "getUserByIdFallback")
    @TimeLimiter(name = "authService")
    public UserResponse getUserById(Long id, String authorizationHeader) {
        try {
            return restClient.get()
                .uri("/api/auth/users/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                .retrieve()
                .body(UserResponse.class);
        } catch (Exception e) {
            log.error("Failed to get user by id from auth service: {}", e.getMessage());
            throw new RuntimeException("Failed to get user from auth service", e);
        }
    }

    private CurrentUserDto validateTokenFallback(String authorizationHeader, Throwable t) {
        log.warn("CircuitBreaker fallback triggered for validateToken: {}", t.getMessage());
        throw new RuntimeException("Auth service is currently unavailable. Please try again later.", t);
    }

    private UserResponse getUserByIdFallback(Long id, String authorizationHeader, Throwable t) {
        log.warn("CircuitBreaker fallback triggered for getUserById: {}", t.getMessage());
        throw new RuntimeException("Auth service is currently unavailable. Please try again later.", t);
    }

    private CurrentUserDto mapToCurrentUserDto(UserResponse response) {
        return CurrentUserDto.builder()
            .id(response.getId())
            .username(response.getUsername())
            .email(response.getEmail())
            .fullName(response.getFullName())
            .roles(response.getRoles())
            .build();
    }
}