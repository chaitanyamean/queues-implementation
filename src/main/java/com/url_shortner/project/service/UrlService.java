package com.url_shortner.project.service;

import com.url_shortner.project.dto.BatchUrlRequestDto;
import com.url_shortner.project.dto.BatchUrlResponseDto;
import com.url_shortner.project.dto.UrlRequestDto;
import com.url_shortner.project.dto.UrlResponseDto;
import com.url_shortner.project.dto.PageResponseDto;

import java.util.List;

public interface UrlService {
    UrlResponseDto shortenUrl(UrlRequestDto request, Long userId);

    List<BatchUrlResponseDto> shortenBatch(BatchUrlRequestDto request, Long userId);

    String getOriginalUrl(String shortCode, String password);

    PageResponseDto<UrlResponseDto> getUrlsByUserId(Long userId, int pageNo, int pageSize);

    // This method handles the FAST write (Redis)
    void incrementVisit(String shortCode);

    void deleteUrl(String shortCode, Long userId);

    UrlResponseDto editUrl(String shortCode, UrlRequestDto request, Long userId);
}
