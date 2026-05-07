package ru.job4j.auth.dto.response;

import ru.job4j.auth.entity.RoleName;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Set<RoleName> roles;
    private LocalDateTime createdAt;
}
