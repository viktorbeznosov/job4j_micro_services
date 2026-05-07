package ru.job4j.auth.service;

import ru.job4j.auth.entity.RoleEntity;
import ru.job4j.auth.entity.RoleName;
import ru.job4j.auth.entity.UserEntity;
import ru.job4j.auth.repository.RoleRepository;
import ru.job4j.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

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
                    .roles(new HashSet<>())
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
                    .roles(new HashSet<>())
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
                    .roles(new HashSet<>())
                    .build();
            manager.getRoles().add(roleRepository.findByName(RoleName.ROLE_MANAGER).get());
            userRepository.save(manager);
        }
    }
}