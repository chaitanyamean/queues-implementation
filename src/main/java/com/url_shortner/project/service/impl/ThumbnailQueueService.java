package com.url_shortner.project.service.impl;

import com.url_shortner.project.service.PubSubService;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ThumbnailQueueService {

    private final PubSubService pubSubService;
    private final com.url_shortner.project.repository.ImageRepository imageRepository; // Inject Repository
    private final com.url_shortner.project.service.ImageStatusService imageStatusService; // Inject Status Service
    private final com.url_shortner.project.service.WebhookService webhookService; // Inject Webhook Service

    private static final String EVENT_IMAGE_UPLOADED = "IMAGE_UPLOADED";

    @PostConstruct
    public void startWorkers() {
        // Subscribe our "workers" (functions) to the event
        pubSubService.subscribe(EVENT_IMAGE_UPLOADED, this::generateThumbnail);
        pubSubService.subscribe(EVENT_IMAGE_UPLOADED, this::logUpload);
        pubSubService.subscribe(EVENT_IMAGE_UPLOADED, this::notifyAdmin);
    }

    private void logUpload(Object data) {
        if (!(data instanceof Long))
            return;
        Long imageId = (Long) data;
        try {
            System.out.println("   [Log-Worker] 📊 Logging analytics for ID: " + imageId);

            // Trigger Webhook
            webhookService.sendAnalytics(imageId, "IMAGE_UPLOADED");

            Thread.sleep(1000);
            System.out.println("   [Log-Worker] Logged.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Expects Long (imageId)
    private void generateThumbnail(Object data) {
        if (!(data instanceof Long))
            return;
        Long imageId = (Long) data;
        try {
            var imageOpt = imageRepository.findById(imageId);
            if (imageOpt.isEmpty())
                return;
            var image = imageOpt.get();

            System.out.println("   [Thumbnail-Worker] 🐢 Starting generation for ID: " + imageId);

            // Update status to PROCESSING
            image.setStatus("PROCESSING");
            imageRepository.save(image);

            Thread.sleep(3000); // Simulate work

            // Update status to COMPLETED
            image.setStatus("COMPLETED");
            // In a real app, we'd actually generate the thumbnail here and set the path
            image.setThumbnailPath("thumbnails/thumb_" + imageId + ".jpg");
            imageRepository.save(image);

            // Notify waiting clients
            imageStatusService.notify(imageId, "COMPLETED");

            System.out.println("   [Thumbnail-Worker] Finished ID: " + imageId);
        } catch (Exception e) {
            System.err.println("   [Thumbnail-Worker] Failed for ID: " + imageId);
            e.printStackTrace();
            // Try to set status FAILED
            imageRepository.findById(imageId).ifPresent(img -> {
                img.setStatus("FAILED");
                imageRepository.save(img);
                imageStatusService.notify(imageId, "FAILED");
            });
        }
    }

    private void notifyAdmin(Object data) {
        if (!(data instanceof Long))
            return;
        Long imageId = (Long) data;
        try {
            System.out.println("   [Notify-Worker] 📧 Notifying admin about ID: " + imageId);
            Thread.sleep(500);
            System.out.println("   [Notify-Worker] Notification sent.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
