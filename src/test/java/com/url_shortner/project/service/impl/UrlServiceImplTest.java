package com.url_shortner.project.service.impl;

import com.url_shortner.project.dto.UrlRequestDto;
import com.url_shortner.project.dto.UrlResponseDto;
import com.url_shortner.project.entity.UrlEntity;
import com.url_shortner.project.entity.UserEntity;
import com.url_shortner.project.repository.UrlRepository;
import com.url_shortner.project.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceImplTest {

    @Mock
    private UrlRepository urlRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UrlServiceImpl urlService;

    private UserEntity user;
    private String apiKey = "test-api-key";

    @BeforeEach
    void setUp() {
        user = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .apiKey(apiKey)
                .build();
    }

    @Test
    void testShortenUrl_WithExpiryDate() {
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://google.com");
        request.setExpiryDate(LocalDateTime.now().plusDays(7));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(urlRepository.findByShortCode(any())).thenReturn(Optional.empty()); // unique code
        when(urlRepository.save(any(UrlEntity.class))).thenAnswer(invocation -> {
            UrlEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        UrlResponseDto response = urlService.shortenUrl(request, 1L);

        assertNotNull(response);
        verify(urlRepository).save(any(UrlEntity.class));
    }

    @Test
    void testGetOriginalUrl_Expired() {
        String shortCode = "expired";
        String password = "thisispassword";
        UrlEntity entity = UrlEntity.builder()
                .shortCode(shortCode)
                .originalUrl("https://expired.com")
                .isActive(true)
                .originalUrl("https://expired.com")
                .isActive(true)
                .expiryDate(LocalDateTime.now().minusDays(1)) // Expired
                .build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(entity));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.getOriginalUrl(shortCode, password);
        });

        assertEquals(HttpStatus.GONE, exception.getStatusCode());
        assertEquals("URL has expired", exception.getReason());
    }

    @Test
    void testGetOriginalUrl_NotExpired() {
        String shortCode = "valid";
        String password = "thisispassword";

        UrlEntity entity = UrlEntity.builder()
                .shortCode(shortCode)
                .originalUrl("https://valid.com")
                .isActive(true)
                .visits(0L)
                .visits(0L)
                .expiryDate(LocalDateTime.now().plusDays(1)) // Not Expired
                .build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(entity));

        String originalUrl = urlService.getOriginalUrl(shortCode, password);

        assertEquals("https://valid.com", originalUrl);
        verify(urlRepository).save(entity); // Should update last_accessed_at/visits
    }

    @Test
    void testShortenUrl_DuplicateCustomCode() {
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://google.com");
        request.setCustomCode("duplicate");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(urlRepository.findByShortCode("duplicate")).thenReturn(Optional.of(new UrlEntity()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.shortenUrl(request, 1L);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Custom code already exists", exception.getReason());
    }

    @Test
    void testShortenUrl_ValidCustomCode() {
        UrlRequestDto request = new UrlRequestDto();
        request.setUrl("https://google.com");
        request.setCustomCode("new-code");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(urlRepository.findByShortCode("new-code")).thenReturn(Optional.empty());
        when(urlRepository.save(any(UrlEntity.class))).thenAnswer(invocation -> {
            UrlEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        UrlResponseDto response = urlService.shortenUrl(request, 1L);

        assertNotNull(response);
        assertEquals("new-code", response.getShortCode());
        verify(urlRepository).save(any(UrlEntity.class));
    }

    @Test
    void testEditUrl_Success() {
        String shortCode = "code123";
        UrlRequestDto request = new UrlRequestDto();
        request.setExpiryDate(LocalDateTime.now().plusDays(10));

        UrlEntity existingEntity = UrlEntity.builder()
                .shortCode(shortCode)
                .originalUrl("https://original.com")
                .isActive(true)
                .build();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.of(existingEntity));
        when(urlRepository.save(any(UrlEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlResponseDto response = urlService.editUrl(shortCode, request, 1L);

        assertNotNull(response);
        assertEquals("success", response.getStatus());
        assertEquals("data updated successfully", response.getMessage());
        assertEquals(shortCode, response.getShortCode());
        verify(urlRepository).save(existingEntity);
    }

    @Test
    void testEditUrl_NotFound() {
        String shortCode = "unknown";
        UrlRequestDto request = new UrlRequestDto();

        when(urlRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            urlService.editUrl(shortCode, request, 1L);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("URL not found", exception.getReason());
    }
}
