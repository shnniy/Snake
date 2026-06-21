package com.snakegame.web.config;

import com.snakegame.web.handler.GameWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 原生 WebSocket 配置（不使用 STOMP）。
 * 提供 /ws/game 端点用于游戏通信。
 */
@Configuration
@EnableWebSocket
public class RawWebSocketConfig implements WebSocketConfigurer {

    private final GameWebSocketHandler gameHandler;

    public RawWebSocketConfig(GameWebSocketHandler gameHandler) {
        this.gameHandler = gameHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gameHandler, "/ws/game")
                .setAllowedOriginPatterns("*");
    }
}
