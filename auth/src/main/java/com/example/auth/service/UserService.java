package com.example.auth.service;

import com.example.auth.dto.response.UserResponse;
import com.example.auth.entity.RoleName;
import com.example.auth.entity.UserEntity;
import com.example.auth.exception.ResourceNotFoundException;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    public UserResponse getUserById(Long id, String currentUsername) {
        UserEntity currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_ADMIN);
        boolean isSelf = currentUser.getId().equals(id);

        if (!isAdmin && !isSelf) {
            throw new org.springframework.security.access.AccessDeniedException("Access denied: You can only view your own profile");
        }

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
