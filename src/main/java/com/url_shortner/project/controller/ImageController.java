package com.url_shortner.project.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.url_shortner.project.service.PubSubService;

@RestController
public class ImageController {

    private final com.url_shortner.project.repository.ImageRepository imageRepository;
    private final com.url_shortner.project.repository.UserRepository userRepository;
    private final PubSubService pubSubService;

    private final com.url_shortner.project.service.ImageStatusService imageStatusService;

    public ImageController(PubSubService pubSubService,
            com.url_shortner.project.repository.ImageRepository imageRepository,
            com.url_shortner.project.repository.UserRepository userRepository,
            com.url_shortner.project.service.ImageStatusService imageStatusService) {
        this.pubSubService = pubSubService;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.imageStatusService = imageStatusService;
    }

    @PostMapping("/upload")
    public String uploadImage(@RequestParam("image") MultipartFile image, java.security.Principal principal) {
        try {
            Long userId = Long.parseLong(principal.getName());
            // Save the image to a temporary location
            String imagePath = saveImageToDisk(image);

            // Save to DB
            com.url_shortner.project.entity.ImageEntity imageEntity = com.url_shortner.project.entity.ImageEntity
                    .builder()
                    .user(userRepository.findById(userId).orElseThrow())
                    .filePath(imagePath)
                    .thumbnailPath(null) // Only null for now
                    .build();

            imageRepository.save(imageEntity);

            // Publish event to trigger async processing
            pubSubService.publish("IMAGE_UPLOADED", imageEntity.getId());

            return "Image uploaded successfully. Saved to DB with ID: " + imageEntity.getId();
        } catch (Exception e) {
            return "Failed to upload image: " + e.getMessage();
        }
    }

    private String saveImageToDisk(MultipartFile image) throws java.io.IOException {
        String uploadDir = "uploads/";
        java.io.File dir = new java.io.File(uploadDir);
        if (!dir.exists())
            dir.mkdirs();

        String fileName = System.currentTimeMillis() + "_" + image.getOriginalFilename();
        java.nio.file.Path filePath = java.nio.file.Paths.get(uploadDir + fileName);
        java.nio.file.Files.copy(image.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return filePath.toString();
    }

    @PostMapping("/enqueue")
    public String enqueue(@org.springframework.web.bind.annotation.RequestBody EnqueueRequest request,
            @org.springframework.web.bind.annotation.RequestHeader("Authorization") String authorization) {
        long start = System.currentTimeMillis();
        System.out.println("[API] Request received for: " + request.url());
        System.out.println("[API] Authorization header received: " + (authorization != null ? "Yes" : "No"));

        // This is non-blocking (just publishing event)
        pubSubService.publish("IMAGE_UPLOADED", request.url());

        long end = System.currentTimeMillis();
        System.out.println("[API] Response sent for: " + request.url() + " in " + (end - start) + "ms");
        return "Url event published to all subscribers";
    }

    public record EnqueueRequest(String url) {
    }

    @org.springframework.web.bind.annotation.GetMapping("/status/{id}")
    public org.springframework.web.context.request.async.DeferredResult<String> checkStatus(
            @org.springframework.web.bind.annotation.PathVariable Long id) {
        return imageStatusService.subscribe(id);
    }
}
