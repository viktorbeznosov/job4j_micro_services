package com.example.payment.security;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class CurrentUserDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private Set<String> roles;
}
