package ru.job4j.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import ru.job4j.auth.dto.response.UserResponse;
import ru.job4j.auth.entity.RoleEntity;
import ru.job4j.auth.entity.RoleName;
import ru.job4j.auth.entity.UserEntity;
import ru.job4j.auth.exception.ResourceNotFoundException;
import ru.job4j.auth.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity adminUser;
    private UserEntity regularUser;
    private RoleEntity adminRole;
    private RoleEntity userRole;

    @BeforeEach
    void setUp() {
        adminRole = RoleEntity.builder()
                .id(1)
                .name(RoleName.ROLE_ADMIN)
                .build();

        userRole = RoleEntity.builder()
                .id(2)
                .name(RoleName.ROLE_USER)
                .build();

        adminUser = new UserEntity();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword("password");
        adminUser.setFullName("Admin User");
        adminUser.setRoles(new HashSet<>());
        adminUser.getRoles().add(adminRole);
        adminUser.setCreatedAt(LocalDateTime.now());
        adminUser.setUpdatedAt(LocalDateTime.now());

        regularUser = new UserEntity();
        regularUser.setId(2L);
        regularUser.setUsername("user");
        regularUser.setEmail("user@example.com");
        regularUser.setPassword("password");
        regularUser.setFullName("Regular User");
        regularUser.setRoles(new HashSet<>());
        regularUser.getRoles().add(userRole);
        regularUser.setCreatedAt(LocalDateTime.now());
        regularUser.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void getAllUsersSuccess() {
        when(userRepository.findAll()).thenReturn(Arrays.asList(adminUser, regularUser));

        List<UserResponse> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getUserByIdAsAdminSuccess() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        UserResponse result = userService.getUserById(2L, "admin");

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void getUserByIdAsSelfSuccess() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

        UserResponse result = userService.getUserById(2L, "user");

        assertNotNull(result);
        assertEquals(2L, result.getId());
    }

    @Test
    void getUserByIdAccessDenied() {
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(regularUser));

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> userService.getUserById(1L, "user"));

        assertTrue(exception.getMessage().contains("Access denied"));

        verify(userRepository).findByUsername("user");
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getUserByIdUserNotFound() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(999L, "admin"));

        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void getUserByIdCurrentUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserById(1L, "unknown"));

        assertEquals("Current user not found", exception.getMessage());
    }
}