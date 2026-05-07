package ru.job4j.payment.security;

import ru.job4j.payment.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthClient {

    private final RestClient restClient;

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
