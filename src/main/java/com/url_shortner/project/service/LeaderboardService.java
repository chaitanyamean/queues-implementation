package com.url_shortner.project.service;

import com.url_shortner.project.websocket.LeaderboardHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class LeaderboardService {

    private final LeaderboardHandler leaderboardHandler;
    private final Random random = new Random();

    public LeaderboardService(LeaderboardHandler leaderboardHandler) {
        this.leaderboardHandler = leaderboardHandler;
    }

    // Push updates every 2 seconds
    @Scheduled(fixedRate = 2000)
    public void pushLeaderboardUpdates() {
        // Simulate a leaderboard JSON
        String json = String.format("{\"topPlayer\": \"Player%d\", \"score\": %d, \"timestamp\": %d}",
                random.nextInt(100),
                random.nextInt(10000),
                System.currentTimeMillis());

        leaderboardHandler.broadcast(json);
        // System.out.println("Broadcasted: " + json);
    }
}
