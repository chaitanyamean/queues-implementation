package com.url_shortner.project.config;

import com.url_shortner.project.websocket.LeaderboardHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final LeaderboardHandler leaderboardHandler;

    public WebSocketConfig(LeaderboardHandler leaderboardHandler) {
        this.leaderboardHandler = leaderboardHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(leaderboardHandler, "/ws/leaderboard")
                .setAllowedOrigins("*"); // Allow all origins for testing
    }
}
