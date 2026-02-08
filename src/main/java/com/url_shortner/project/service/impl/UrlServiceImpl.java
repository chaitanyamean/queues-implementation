package com.url_shortner.project.service.impl;

import com.url_shortner.project.dto.*;
import com.url_shortner.project.entity.UrlEntity;
import com.url_shortner.project.repository.UrlRepository;
import com.url_shortner.project.service.UrlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import com.url_shortner.project.entity.Role;
import com.url_shortner.project.entity.UserEntity;
import java.security.SecureRandom;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class UrlServiceImpl implements UrlService {

    private final UrlRepository urlRepository;
    private final com.url_shortner.project.repository.UserRepository userRepository;
    private final Map<String, String> urlCache = new ConcurrentHashMap<>();

    // 📊 METRICS COUNTERS
    private final AtomicInteger cacheHits = new AtomicInteger(0);
    private final AtomicInteger cacheMisses = new AtomicInteger(0);

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();

    @Autowired
    private StringRedisTemplate redisStatsTemplate;

    @Value("${app.base-url:http://localhost:8080/}")
    private String baseUrl;

    @Override
    @Transactional
    public UrlResponseDto shortenUrl(UrlRequestDto request, Long userId) {
        System.out.println("request: " + request + " userId: " + userId);
        if (request == null || request.getUrl() == null || request.getUrl().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "URL cannot be empty");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        String code = request.getCustomCode();
        if (code != null && !code.trim().isEmpty()) {
            if (urlRepository.findByShortCode(code).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Custom code already exists");
            }
        } else {
            code = generateUniqueCode();
        }

        UrlEntity entity = UrlEntity.builder()
                .originalUrl(request.getUrl())
                .shortCode(code)
                .visits(0L)
                .user(user)
                .isActive(true)
                .customCode(request.getCustomCode())
                .expiryDate(request.getExpiryDate())
                .password(request.getPassword())
                .build();

        log.debug("Generated entity: {}", entity);

        try {
            UrlEntity savedEntity = urlRepository.save(entity);

            return UrlResponseDto.builder()
                    .originalUrl(savedEntity.getOriginalUrl())
                    .shortCode(savedEntity.getShortCode())
                    .shortUrl(baseUrl + savedEntity.getShortCode())
                    .build();

        } catch (DataIntegrityViolationException e) {
            log.warn("Race condition detected for URL: {}. Fetching existing record.", request.getUrl());
            // Fallback: try to find the existing one again
            return urlRepository.findByOriginalUrl(request.getUrl())
                    .map(existing -> UrlResponseDto.builder()
                            .originalUrl(existing.getOriginalUrl())
                            .shortCode(existing.getShortCode())
                            .shortUrl(baseUrl + "redirect?code=" + existing.getShortCode())
                            .build())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Unexpected error while saving URL"));
        } catch (Exception e) {
            log.error("Error creating short URL", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "An error occurred while processing the request");
        }

    }

    @Override
    @Transactional
    @Cacheable(value = "urls", key = "#shortCode")
    public String getOriginalUrl(String shortCode, String password) {
        log.debug("Fetching URL for code: {}", shortCode, password);

        System.out.println("🐢 Cache Miss! Fetching from Database for: " +
                shortCode);

        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElse(null);

        if (entity == null) {
            return null;
        }

        log.debug("Entity get password " + entity.getPassword());
        if (entity.getPassword() != null) {
            if (!entity.getPassword().equals(password)) {
                throw new RuntimeException("Not able to access, Password not matching: " + shortCode);
            }
        }
        if (!entity.isActive()) {
            throw new RuntimeException("URL not found for code: " + shortCode);
        }

        if (entity.getExpiryDate() != null) {
            if (entity.getExpiryDate().isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.GONE, "URL has expired");
            }
        }

        // urlCache.put(shortCode, entity.getOriginalUrl());

        // if (entity.getVisits() == null) {
        // entity.setVisits(1L);
        // } else {
        // System.out.println("get visits "+ entity.getVisits());
        // entity.setVisits(entity.getVisits() + 1);
        // }
        // entity.setLast_accessed_at(LocalDateTime.now());
        // urlRepository.save(entity);

        return entity.getOriginalUrl();
    }

    // This method handles the FAST write (Redis)
    @Override
    public void incrementVisit(String shortCode) {
        // 1. Increment the counter in Redis (Key: "visits:abc12")
        redisStatsTemplate.opsForValue().increment("visits:" + shortCode);

        // 2. Add to a "Dirty Set" so we know WHICH keys changed
        redisStatsTemplate.opsForSet().add("dirty_urls", shortCode);
    }

    // Helper to print stats
    public void printStats() {
        int total = cacheHits.get() + cacheMisses.get();
        double hitRatio = (total == 0) ? 0 : ((double) cacheHits.get() / total) * 100;

        System.out.println("=================================");
        System.out.println("📊 CACHE PERFORMANCE REPORT");
        System.out.println("Total Requests: " + total);
        System.out.println("✅ Cache Hits:   " + cacheHits.get());
        System.out.println("❌ Cache Misses: " + cacheMisses.get());
        System.out.println("🚀 Hit Ratio:    " + String.format("%.2f", hitRatio) + "%");
        System.out.println("=================================");
    }

    public void resetStats() {
        cacheHits.set(0);
        cacheMisses.set(0);
        urlCache.clear(); // Wipe cache to simulate "No Cache" scenario
    }

    @Override
    @Transactional
    public void deleteUrl(String shortCode, Long userId) {
        log.debug("Deleting URL with code: {}", shortCode);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new RuntimeException("URL not found"));

        if (!entity.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("User is not authorized to delete this URL");
        }
        entity.setActive(false);
        urlRepository.save(entity);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generateRandomString(6);
        } while (urlRepository.findByShortCode(code).isPresent());
        return code;
    }

    private String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    @Override
    public List<BatchUrlResponseDto> shortenBatch(BatchUrlRequestDto request, Long userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!user.getRoles().contains(Role.ENTERPRISE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Enterprise role required");
        }

        List<BatchUrlResponseDto> responseList = new ArrayList<>();

        for (String url : request.getUrls()) {
            try {
                UrlRequestDto singleRequest = new UrlRequestDto();
                singleRequest.setUrl(url);

                UrlResponseDto singleResponse = shortenUrl(singleRequest, userId);

                responseList.add(BatchUrlResponseDto.builder()
                        .originalUrl(url)
                        .shortUrl(singleResponse.getShortUrl())
                        .status("SUCCESS")
                        .build());

            } catch (Exception e) {
                responseList.add(BatchUrlResponseDto.builder()
                        .originalUrl(url)
                        .error(e.getMessage())
                        .status("FAILURE")
                        .build());
            }
        }
        return responseList;
    }

    @Override
    public PageResponseDto<UrlResponseDto> getUrlsByUserId(Long userId, int pageNo, int pageSize) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        Pageable pageable = PageRequest.of(pageNo, pageSize, Sort.by("createdAt").descending());

        Page<UrlEntity> urls = urlRepository.findByUser(user, pageable);

        List<UrlResponseDto> content = urls.stream()
                .map(entity -> UrlResponseDto.builder()
                        .originalUrl(entity.getOriginalUrl())
                        .shortCode(entity.getShortCode())
                        .shortUrl(baseUrl + entity.getShortCode())
                        .build())
                .collect(Collectors.toList());

        return PageResponseDto.<UrlResponseDto>builder()
                .content(content)
                .pageNo(urls.getNumber())
                .pageSize(urls.getSize())
                .totalElements(urls.getTotalElements())
                .totalPages(urls.getTotalPages())
                .last(urls.isLast())
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = "urls", key = "#shortCode")
    public UrlResponseDto editUrl(String shortCode, UrlRequestDto request, Long userId) {
        UrlEntity entity = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found"));

        entity.setOriginalUrl(request.getUrl());
        // entity.setCustomCode(request.getCustomCode());
        entity.setExpiryDate(request.getExpiryDate());

        UrlEntity updatedEntity = urlRepository.save(entity);

        return UrlResponseDto.builder()
                .originalUrl(updatedEntity.getOriginalUrl())
                .shortCode(updatedEntity.getShortCode())
                .shortUrl(baseUrl + updatedEntity.getShortCode())
                .status("success")
                .message("data updated successfully")
                .build();
    }

}
