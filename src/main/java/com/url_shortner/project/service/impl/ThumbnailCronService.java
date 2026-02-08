package com.url_shortner.project.service.impl;

import com.url_shortner.project.entity.ImageEntity;
import com.url_shortner.project.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ThumbnailCronService {

    private final ImageRepository imageRepository;

    @Scheduled(fixedRate = 300000)
    public void processPendingThumbnails() {
        log.info("[Cron] Checking for images without thumbnails...");

        List<ImageEntity> pendingImages = imageRepository.findByThumbnailPathIsNull();

        if (pendingImages.isEmpty()) {
            log.info("[Cron] No pending images found.");
            return;
        }

        for (ImageEntity image : pendingImages) {
            try {
                processImage(image);
                logSuccess(image);
            } catch (Exception e) {
                log.error("[Cron] Failed to process image ID: {}", image.getId(), e);
                logFailure(image, e);
            }
        }
    }

    private void logSuccess(ImageEntity image) {
        try {
            System.out.println("[Analytics] Thumbnail generation SUCCESS for Image ID: " + image.getId());
            Thread.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void logFailure(ImageEntity image, Exception e) {
        try {
            System.out.println("[Analytics] Thumbnail generation FAILED for Image ID: " + image.getId() + ". Error: "
                    + e.getMessage());
            Thread.sleep(2);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void processImage(ImageEntity image) throws IOException {
        long start = System.currentTimeMillis();
        File sourceFile = new File(image.getFilePath());

        if (!sourceFile.exists()) {
            log.error("File not found: {}", image.getFilePath());
            return;
        }

        BufferedImage original = ImageIO.read(sourceFile);
        BufferedImage resized = new BufferedImage(300, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, 300, 300, null);
        g.dispose();

        String thumbnailDir = "thumbnails/";
        File dir = new File(thumbnailDir);
        if (!dir.exists())
            dir.mkdirs();

        String thumbnailName = "thumb_" + sourceFile.getName();
        File destFile = new File(thumbnailDir + thumbnailName);
        ImageIO.write(resized, "jpg", destFile);

        image.setThumbnailPath(destFile.getAbsolutePath());
        imageRepository.save(image);

        long duration = System.currentTimeMillis() - start;
        log.info("[Cron] Generated thumbnail for ID: {} in {}ms", image.getId(), duration);
    }

    private void logAfterImageUpload(ImageEntity image) {
        log.info("[Cron] Logging upload analytics for: {}", image.getFilePath());
    }
}
