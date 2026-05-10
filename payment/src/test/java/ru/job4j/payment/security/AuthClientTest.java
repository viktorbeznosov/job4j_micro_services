package ru.job4j.payment.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import ru.job4j.payment.dto.response.UserResponse;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthClientTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AuthClient authClient;

    @BeforeEach
    void setUp() {
        authClient = new AuthClient(restClient);
    }

    @Test
    void validateTokenAndGetUserSuccess() {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/api/auth/internal/validate"))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(any(), any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(UserResponse.class)).thenReturn(userResponse);

        CurrentUserDto result = authClient.validateTokenAndGetUser("Bearer token");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
    }

    @Test
    void validateTokenAndGetUserNullResponse() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/api/auth/internal/validate"))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(any(), any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(UserResponse.class)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authClient.validateTokenAndGetUser("Bearer token"));

        assertEquals("Invalid or expired token", exception.getMessage());
    }

    @Test
    void getUserByIdSuccess() {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .roles(Set.of("ROLE_USER"))
                .build();

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/api/auth/users/{id}"), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(any(), any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(UserResponse.class)).thenReturn(userResponse);

        UserResponse result = authClient.getUserById(1L, "Bearer token");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserByIdThrowsRuntimeException() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(eq("/api/auth/users/{id}"), eq(1L))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.header(any(), any())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenThrow(new RuntimeException("Connection refused"));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authClient.getUserById(1L, "Bearer token"));

        assertTrue(exception.getMessage().contains("Failed to get user"));
    }
}