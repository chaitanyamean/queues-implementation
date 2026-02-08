package com.url_shortner.project.service;

import com.url_shortner.project.repository.ImageRepository;
import com.url_shortner.project.entity.ImageEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

@Service
public class ImageStatusService {

    private final Map<Long, DeferredResult<String>> pendingRequests = new ConcurrentHashMap<>();
    private final ImageRepository imageRepository;

    public ImageStatusService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public DeferredResult<String> subscribe(Long imageId) {
        DeferredResult<String> output = new DeferredResult<>(30000L, "TIMEOUT");

        pendingRequests.put(imageId, output);

        // Check DB state immediately to handle race conditions or retries
        Optional<ImageEntity> imageOpt = imageRepository.findById(imageId);
        if (imageOpt.isPresent()) {
            String status = imageOpt.get().getStatus();
            if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                output.setResult(status);
            }
        }

        output.onCompletion(() -> pendingRequests.remove(imageId));
        output.onTimeout(() -> pendingRequests.remove(imageId));
        output.onError((e) -> pendingRequests.remove(imageId));

        return output;
    }

    public void notify(Long imageId, String status) {
        DeferredResult<String> result = pendingRequests.remove(imageId);
        if (result != null && !result.isSetOrExpired()) {
            result.setResult(status);
        }
    }
}
