package com.url_shortner.project.service;

import com.url_shortner.project.repository.UrlRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AnalyticsSyncService {

    private final UrlRepository urlRepository;
    private final StringRedisTemplate redisStatsTemplate;


    public AnalyticsSyncService(UrlRepository urlRepository, StringRedisTemplate redisStatsTemplate) {
        this.urlRepository = urlRepository;
        this.redisStatsTemplate = redisStatsTemplate;
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void syncVisitsToDatabase() {
        // 1. Get all shortCodes that have been visited recently
        Set<String> dirtyCodes = redisStatsTemplate.opsForSet().members("dirty_urls");

        if (dirtyCodes == null || dirtyCodes.isEmpty()) {
            return; // Nothing to sync
        }
        for (String shortCode : dirtyCodes) {
            // 2. Get the count from Redis
            System.out.println("✅ This are dirty codes " + shortCode + " is in dirtycodes.");

            String countStr = redisStatsTemplate.opsForValue().get("visits:" + shortCode);
            System.out.println("Redis Count " + countStr);
            if (countStr == null) continue;

            int visitsToAdd = Integer.parseInt(countStr);

            // 3. Update Postgres (Write a custom query in Repo!)
            // UPDATE urls SET visits = visits + :count WHERE short_code = :code
            urlRepository.incrementVisits(shortCode, visitsToAdd);

            // 4. Cleanup Redis
            // We delete the count and remove from dirty set
            redisStatsTemplate.delete("visits:" + shortCode);
            redisStatsTemplate.opsForSet().remove("dirty_urls", shortCode);
        }

        System.out.println("✅ Synced " + dirtyCodes.size() + " URL stats to DB.");
    }
}
