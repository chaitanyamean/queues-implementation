package com.url_shortner.project.repository;

import com.url_shortner.project.entity.UrlEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import com.url_shortner.project.entity.UserEntity;
import java.util.List;
import java.util.Optional;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {
    Optional<UrlEntity> findByShortCode(String shortCode);

    Optional<UrlEntity> findByOriginalUrl(String originalUrl);

    Page<UrlEntity> findByUser(UserEntity user, Pageable pageable);

    @Modifying
    @Query("UPDATE UrlEntity u SET u.visits = u.visits + :count WHERE u.shortCode = :shortCode")
    void incrementVisits(String shortCode, int count);


}
