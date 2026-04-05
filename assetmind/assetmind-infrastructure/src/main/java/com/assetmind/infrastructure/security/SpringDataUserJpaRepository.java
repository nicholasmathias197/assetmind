package com.assetmind.infrastructure.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataUserJpaRepository extends JpaRepository<UserEntity, String> {
    Optional<UserEntity> findByUsername(String username);

    List<UserEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByRoleIgnoreCase(String role);
}

