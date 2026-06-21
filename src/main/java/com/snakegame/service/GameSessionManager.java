package com.snakegame.service;

import com.snakegame.engine.*;
import com.snakegame.persistence.service.HighScoreService;
import com.snakegame.web.dto.EventMessage;
import com.snakegame.web.dto.GameStateMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 游戏会话管理器。
 * 管理多个游戏会话（每个浏览器标签页一个），
 * 负责会话的创建、查找和销毁，以及在游戏结束时自动保存分数。
 */
@Service
public class GameSessionManager {

    private final Map<String, GameEngine> sessions = new ConcurrentHashMap<>();
    private final Map<String, GameEventListener> listeners = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final HighScoreService highScoreService;

    public GameSessionManager(SimpMessagingTemplate messagingTemplate, HighScoreService highScoreService) {
        this.messagingTemplate = messagingTemplate;
        this.highScoreService = highScoreService;
    }

    /**
     * 创建新的游戏会话。
     *
     * @return 新会话的 ID
     */
    public String createSession() {
        String gameId = UUID.randomUUID().toString().substring(0, 8);

        GameConfig config = GameConfig.defaultConfig();
        GameEngine engine = new GameEngine(config);

        // 创建 WebSocket 事件监听器
        GameEventListener listener = new GameEventListener() {
            @Override
            public void onStateUpdate(GameStateSnapshot snapshot) {
                GameStateMessage msg = GameStateMessage.from(
                    gameId,
                    snapshot.snakeBody(),
                    snapshot.foodPosition(),
                    snapshot.foodType(),
                    snapshot.score(),
                    snapshot.highScore(),
                    snapshot.gameState(),
                    snapshot.speedBoost()
                );
                messagingTemplate.convertAndSend("/topic/game/" + gameId, msg);
            }

            @Override
            public void onEvent(GameEvent event) {
                GameEngine eng = sessions.get(gameId);
                int score = eng != null ? eng.getScoreTracker().getCurrentScore() : 0;
                int highScore = eng != null ? eng.getScoreTracker().getHighScore() : 0;

                // 游戏结束时自动保存分数到排行榜
                if (event == GameEvent.GAME_OVER && eng != null && score > 0) {
                    try {
                        highScoreService.saveScore("玩家", score,
                            eng.getSnake().getLength(), eng.getFoodEatenCount());
                        System.out.println("[会话管理] 分数已保存: " + score);
                    } catch (Exception e) {
                        System.err.println("[会话管理] 保存分数失败: " + e.getMessage());
                    }
                }

                EventMessage msg = EventMessage.of(
                    gameId,
                    event.name(),
                    score,
                    highScore,
                    getEventMessage(event, score)
                );
                messagingTemplate.convertAndSend("/topic/events/" + gameId, msg);
            }
        };

        engine.addListener(listener);
        engine.init();

        sessions.put(gameId, engine);
        listeners.put(gameId, listener);

        System.out.println("[会话管理] 创建新会话: " + gameId);
        return gameId;
    }

    /**
     * 获取游戏会话。
     */
    public GameEngine getSession(String gameId) {
        return sessions.get(gameId);
    }

    /**
     * 移除游戏会话。
     */
    public void removeSession(String gameId) {
        GameEngine engine = sessions.remove(gameId);
        GameEventListener listener = listeners.remove(gameId);
        if (engine != null) {
            engine.removeListener(listener);
            engine.shutdown();
        }
        System.out.println("[会话管理] 移除会话: " + gameId);
    }

    /**
     * 处理用户输入（方向 + 加速）。
     */
    public void handleInput(String gameId, String direction, boolean speedBoost) {
        GameEngine engine = sessions.get(gameId);
        if (engine == null) return;

        try {
            Direction dir = Direction.valueOf(direction.toUpperCase());
            engine.setDirection(dir);
        } catch (IllegalArgumentException e) {
            System.err.println("[会话管理] 无效的方向: " + direction);
        }

        engine.setSpeedBoost(speedBoost);
    }

    /**
     * 处理游戏命令。
     */
    public void handleCommand(String gameId, String action) {
        GameEngine engine = sessions.get(gameId);
        if (engine == null) return;

        switch (action.toUpperCase()) {
            case "START"  -> engine.start();
            case "PAUSE"  -> engine.pause();
            case "RESUME" -> engine.resume();
            case "RESTART" -> {
                engine.reset();
                engine.init();
                engine.start();
            }
            default -> System.err.println("[会话管理] 未知命令: " + action);
        }
    }

    /**
     * 获取事件对应的描述消息。
     */
    private String getEventMessage(GameEvent event, int score) {
        return switch (event) {
            case FOOD_EATEN  -> "吃到食物！";
            case GAME_OVER   -> "游戏结束！最终得分: " + score;
            case HIGH_SCORE  -> "新最高分: " + score + "!";
            case GAME_STARTED -> "游戏开始！";
            case GAME_PAUSED  -> "游戏暂停";
            case GAME_RESUMED -> "游戏继续";
        };
    }

    /**
     * 获取当前活跃会话数。
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
