package com.snakegame.web.controller;

import com.snakegame.service.GameSessionManager;
import com.snakegame.web.dto.CommandMessage;
import com.snakegame.web.dto.InputMessage;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * WebSocket 游戏控制器。
 * 处理来自客户端的 STOMP 消息。
 */
@Controller
public class GameController {

    private final GameSessionManager sessionManager;

    public GameController(GameSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    /**
     * 处理用户输入消息。
     * 客户端发送到 /app/game/{gameId}/input
     */
    @MessageMapping("/game/{gameId}/input")
    public void handleInput(@DestinationVariable String gameId, InputMessage input) {
        if (input.getDirection() != null) {
            sessionManager.handleInput(gameId, input.getDirection(), input.isSpeedBoost());
        }
    }

    /**
     * 处理游戏命令消息。
     * 客户端发送到 /app/game/{gameId}/command
     */
    @MessageMapping("/game/{gameId}/command")
    public void handleCommand(@DestinationVariable String gameId, CommandMessage command) {
        if (command.getAction() != null) {
            sessionManager.handleCommand(gameId, command.getAction());
        }
    }
}
