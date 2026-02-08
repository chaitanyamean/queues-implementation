package com.url_shortner.project.repository;

import com.url_shortner.project.entity.ImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<ImageEntity, Long> {

    List<ImageEntity> findByThumbnailPathIsNull();
}
