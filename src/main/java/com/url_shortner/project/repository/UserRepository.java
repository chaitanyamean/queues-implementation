package com.url_shortner.project.repository;

import com.url_shortner.project.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByApiKey(String apiKey);

    Optional<UserEntity> findByEmail(String email);
}
