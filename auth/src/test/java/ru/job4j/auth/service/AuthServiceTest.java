package ru.job4j.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.job4j.auth.dto.request.LoginRequest;
import ru.job4j.auth.dto.request.RegisterRequest;
import ru.job4j.auth.dto.response.AuthResponse;
import ru.job4j.auth.dto.response.UserResponse;
import ru.job4j.auth.entity.RoleEntity;
import ru.job4j.auth.entity.RoleName;
import ru.job4j.auth.entity.UserEntity;
import ru.job4j.auth.exception.BadRequestException;
import ru.job4j.auth.exception.UnauthorizedException;
import ru.job4j.auth.repository.RoleRepository;
import ru.job4j.auth.repository.UserRepository;
import ru.job4j.auth.security.JwtService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private UserEntity userEntity;
    private RoleEntity roleEntity;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");

        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        roleEntity = RoleEntity.builder()
                .id(1)
                .name(RoleName.ROLE_USER)
                .build();

        userEntity = new UserEntity();
        userEntity.setId(1L);
        userEntity.setUsername("testuser");
        userEntity.setEmail("test@example.com");
        userEntity.setPassword("encodedPassword");
        userEntity.setFullName("Test User");
        userEntity.setRoles(new HashSet<>());
        userEntity.getRoles().add(roleEntity);
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void registerSuccess() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(roleEntity));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(userEntity);

        UserResponse result = authService.register(registerRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        verify(userRepository).save(any(UserEntity.class));
    }

    @Test
    void registerUsernameAlreadyTaken() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.register(registerRequest));

        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerEmailAlreadyInUse() {
        when(userRepository.existsByUsername(registerRequest.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> authService.register(registerRequest));

        assertEquals("Email is already in use", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loginSuccess() {
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(userRepository.findByUsername(loginRequest.getUsername())).thenReturn(Optional.of(userEntity));
        when(jwtService.generateToken(any(UserEntity.class))).thenReturn("jwt-token");

        AuthResponse result = authService.login(loginRequest);

        assertNotNull(result);
        assertEquals("jwt-token", result.getToken());
    }

    @Test
    void loginInvalidCredentials() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid"));

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.login(loginRequest));

        assertEquals("Invalid username or password", exception.getMessage());
    }

    @Test
    void getCurrentUserSuccess() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(userEntity));

        UserResponse result = authService.getCurrentUser("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getCurrentUserUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class,
                () -> authService.getCurrentUser("unknown"));

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void getUserByIdSuccess() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userEntity));

        UserResponse result = authService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}