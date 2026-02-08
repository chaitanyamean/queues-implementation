package com.url_shortner.project.controller;

import com.url_shortner.project.dto.BatchUrlRequestDto;
import com.url_shortner.project.dto.BatchUrlResponseDto;
import com.url_shortner.project.dto.UrlRequestDto;
import com.url_shortner.project.dto.UrlResponseDto;
import com.url_shortner.project.service.UrlService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;

    @PostMapping("/shorten")
    public ResponseEntity<UrlResponseDto> shortenUrl(@Valid @RequestBody UrlRequestDto request,
            java.security.Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        UrlResponseDto response = urlService.shortenUrl(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/shorten/batch")
    public ResponseEntity<java.util.List<BatchUrlResponseDto>> batchShorten(
            @Valid @RequestBody BatchUrlRequestDto request,
            java.security.Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        java.util.List<BatchUrlResponseDto> response = urlService.shortenBatch(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/redirect")
    public RedirectView redirect(@RequestParam String shortCode, @RequestParam(required = false) String password) {
        System.out.println("shortCode: " + shortCode);
        String originalUrl = urlService.getOriginalUrl(shortCode, password);
        urlService.incrementVisit(shortCode);

        if (originalUrl == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));

        // 3. Set Cache-Control: public, max-age=86400 (24 Hours) 🕒
        // This tells the browser to cache this redirect for 1 day
        headers.setCacheControl(CacheControl.maxAge(24, TimeUnit.HOURS).cachePublic());
        return new RedirectView(originalUrl);
    }

    @DeleteMapping("/shorten/{shortCode}")
    public ResponseEntity<java.util.Map<String, Object>> deleteUrl(@PathVariable String shortCode,
            java.security.Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        urlService.deleteUrl(shortCode, userId);
        return ResponseEntity.ok(java.util.Map.of(
                "status", "success",
                "message", "URL deleted successfully",
                "statusCode", 200));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<com.url_shortner.project.dto.PageResponseDto<UrlResponseDto>> getUrlsByUserId(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.url_shortner.project.dto.PageResponseDto<UrlResponseDto> urls = urlService.getUrlsByUserId(userId, page,
                size);
        return ResponseEntity.ok(urls);
    }

    @PutMapping("/edit/{shortCode}")
    public ResponseEntity<UrlResponseDto> editUrl(@PathVariable String shortCode,
            @Valid @RequestBody UrlRequestDto request,
            java.security.Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        UrlResponseDto response = urlService.editUrl(shortCode, request, userId);
        return ResponseEntity.ok(response);
    }
    // 🔴 1. SYNC ENDPOINT (The Blocking Way)
    @GetMapping("/sync")
    public String syncTask() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        System.out.println("[Sync] Request received at: " + startTime);

        // Simulate a slow task (e.g., generating a PDF)
        performSlowTask();

        long endTime = System.currentTimeMillis();
        System.out.println("[Sync] Task finished at: " + endTime);
        System.out.println("[Sync] Total Duration: " + (endTime - startTime) + "ms");

        return "Done (Slow)";
    }

    // 🟢 2. ASYNC ENDPOINT (The Non-Blocking Way)
    @GetMapping("/async")
    public String asyncTask() {
        long startTime = System.currentTimeMillis();
        System.out.println("[Async] Request received at: " + startTime);

        // Offload the work to a background thread
        CompletableFuture.runAsync(() -> {
            try {
                // This happens in the background!
                System.out.println("   --> [Background Thread] Task Started...");
                performSlowTask();
                System.out.println("   --> [Background Thread] Task Finished!");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        long endTime = System.currentTimeMillis();
        System.out.println("[Async] Response sent at: " + endTime);
        System.out.println("[Async] Total API Duration: " + (endTime - startTime) + "ms");

        return "Accepted (Fast)";
    }

    private void performSlowTask() throws InterruptedException {
        Thread.sleep(3000); // Sleep for 3 seconds
    }

}
