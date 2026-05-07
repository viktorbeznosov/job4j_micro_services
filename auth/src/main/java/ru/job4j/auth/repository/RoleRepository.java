package ru.job4j.auth.repository;

import ru.job4j.auth.entity.RoleEntity;
import ru.job4j.auth.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<RoleEntity, Integer> {
    Optional<RoleEntity> findByName(RoleName name);
}
