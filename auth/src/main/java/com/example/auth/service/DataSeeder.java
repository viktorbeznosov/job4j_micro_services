package com.example.auth.service;

import com.example.auth.entity.RoleEntity;
import com.example.auth.entity.RoleName;
import com.example.auth.entity.UserEntity;
import com.example.auth.repository.RoleRepository;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void seed() {
        if (roleRepository.count() == 0) {
            roleRepository.save(RoleEntity.builder().name(RoleName.ROLE_USER).build());
            roleRepository.save(RoleEntity.builder().name(RoleName.ROLE_ADMIN).build());
            roleRepository.save(RoleEntity.builder().name(RoleName.ROLE_MANAGER).build());
        }

        if (!userRepository.existsByUsername("admin")) {
            UserEntity admin = UserEntity.builder()
                    .username("admin")
                    .email("admin@test.com")
                    .fullName("Администратор")
                    .password(passwordEncoder.encode("admin123"))
                    .build();
            admin.getRoles().add(roleRepository.findByName(RoleName.ROLE_ADMIN).get());
            userRepository.save(admin);
        }

        if (!userRepository.existsByUsername("user")) {
            UserEntity user = UserEntity.builder()
                    .username("user")
                    .email("user@test.com")
                    .fullName("Обычный Пользователь")
                    .password(passwordEncoder.encode("user123"))
                    .build();
            user.getRoles().add(roleRepository.findByName(RoleName.ROLE_USER).get());
            userRepository.save(user);
        }

        if (!userRepository.existsByUsername("manager")) {
            UserEntity manager = UserEntity.builder()
                    .username("manager")
                    .email("manager@test.com")
                    .fullName("Менеджер")
                    .password(passwordEncoder.encode("manager123"))
                    .build();
            manager.getRoles().add(roleRepository.findByName(RoleName.ROLE_MANAGER).get());
            userRepository.save(manager);
        }
    }
}
