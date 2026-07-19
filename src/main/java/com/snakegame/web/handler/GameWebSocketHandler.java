package com.snakegame.web.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.snakegame.engine.*;
import com.snakegame.persistence.service.HighScoreService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原生 WebSocket 游戏处理器。
 * 使用简单的 JSON 协议直接通信，无需 STOMP/SockJS。
 *
 * 协议:
 *   Client→Server: {"action":"START"|"PAUSE"|"RESUME"|"RESTART"|"SET_SIZE"}
 *   Client→Server: {"direction":"UP"|"DOWN"|"LEFT"|"RIGHT","speedBoost":true|false}
 *   Server→Client: {"type":"STATE","snake":[[x,y],...],"food":[x,y],"foodType":"NORMAL","score":0,"highScore":0,"gameState":"RUNNING","speedBoost":false}
 *   Server→Client: {"type":"EVENT","action":"FOOD_EATEN"|"GAME_OVER"|"HIGH_SCORE","score":0,"message":"..."}
 */
@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, GameEngine> engines = new ConcurrentHashMap<>();
    private final Map<String, GameEventListener> listeners = new ConcurrentHashMap<>();
    private final Map<String, GameConfig> configs = new ConcurrentHashMap<>();
    private final HighScoreService highScoreService;

    public GameWebSocketHandler(HighScoreService highScoreService) {
        this.highScoreService = highScoreService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String gameId = session.getId().substring(0, 8);

        GameConfig config = GameConfig.defaultConfig();
        GameEngine engine = new GameEngine(config);
        GameEventListener listener = createGameEventListener(gameId, session, engine);

        sessions.put(gameId, session);
        engines.put(gameId, engine);
        listeners.put(gameId, listener);
        configs.put(gameId, config);

        // 先发送 INIT（在 engine.init() 之前，确保客户端先收到 gameId）
        sendJson(session, Map.of(
            "type", "INIT",
            "gameId", gameId,
            "gridWidth", config.gridWidth(),
            "gridHeight", config.gridHeight(),
            "gameState", "READY"
        ));

        // 再初始化引擎（会触发 STATE 推送）
        engine.addListener(listener);
        engine.init();

        System.out.println("[WebSocket] 新连接: " + gameId);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String gameId = session.getId().substring(0, 8);
        try {
            GameEngine engine = engines.get(gameId);
            if (engine == null) {
                System.err.println("[WebSocket] 未找到引擎: " + gameId);
                return;
            }

            Map<String, Object> msg = objectMapper.readValue(message.getPayload(), Map.class);

            // 处理命令
            if (msg.containsKey("action")) {
                String action = (String) msg.get("action");
                System.out.println("[WebSocket] 收到命令: " + action + " from " + gameId);
                switch (action.toUpperCase()) {
                    case "START" -> {
                        if (engine.getState() == GameState.GAME_OVER) {
                            engine.reset();
                        }
                        engine.init();
                        engine.start();
                    }
                    case "PAUSE" -> engine.pause();
                    case "RESUME" -> engine.resume();
                    case "RESTART" -> {
                        engine.reset();
                        engine.init();
                        engine.start();
                    }
                    case "SET_SIZE" -> {
                        String sizeName = (String) msg.get("size");
                        if (sizeName != null) {
                            changeGameSize(gameId, session, sizeName);
                        } else {
                            System.err.println("[WebSocket] SET_SIZE 缺少 size 字段");
                        }
                    }
                }
            }

            // 处理方向输入
            if (msg.containsKey("direction")) {
                String dirStr = (String) msg.get("direction");
                try {
                    Direction dir = Direction.valueOf(dirStr.toUpperCase());
                    engine.setDirection(dir);
                } catch (IllegalArgumentException e) {
                    System.err.println("无效方向: " + dirStr);
                }
            }

            // 处理加速
            if (msg.containsKey("speedBoost")) {
                engine.setSpeedBoost((Boolean) msg.get("speedBoost"));
            }
        } catch (Exception e) {
            System.err.println("[WebSocket] 处理消息异常: " + e.getMessage());
            e.printStackTrace();
            // 不抛出异常，避免 Spring 关闭 WebSocket 连接
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String gameId = session.getId().substring(0, 8);
        GameEngine engine = engines.remove(gameId);
        GameEventListener listener = listeners.remove(gameId);
        sessions.remove(gameId);
        configs.remove(gameId);

        if (engine != null && listener != null) {
            engine.removeListener(listener);
            engine.shutdown();
        }
        System.out.println("[WebSocket] 断开: " + gameId + " 原因: " + status.getCode() + " " + status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("[WebSocket] 传输错误: " + exception.getMessage());
        exception.printStackTrace();
    }

    // ===== 尺寸切换 =====

    /**
     * 切换游戏窗口大小。
     * 停止旧引擎，创建新引擎（使用新配置），发送 INIT，初始化。
     */
    private synchronized void changeGameSize(String gameId, WebSocketSession session, String sizeName) {
        // 1. 停止并清理旧引擎
        GameEngine oldEngine = engines.get(gameId);
        GameEventListener oldListener = listeners.get(gameId);

        if (oldEngine != null && oldListener != null) {
            oldEngine.removeListener(oldListener);
            oldEngine.shutdown();
        }

        // 2. 创建新配置和新引擎
        GameConfig newConfig = GameConfig.fromSizeName(sizeName);
        GameEngine newEngine = new GameEngine(newConfig);

        // 3. 创建新监听器（复用工厂方法）
        GameEventListener newListener = createGameEventListener(gameId, session, newEngine);

        // 4. 更新映射
        engines.put(gameId, newEngine);
        listeners.put(gameId, newListener);
        configs.put(gameId, newConfig);

        // 5. 发送 INIT（先发送，确保客户端先收到新网格尺寸）
        try {
            sendJson(session, Map.of(
                "type", "INIT",
                "gameId", gameId,
                "gridWidth", newConfig.gridWidth(),
                "gridHeight", newConfig.gridHeight(),
                "gameState", "READY"
            ));
        } catch (IOException e) {
            System.err.println("[WebSocket] 发送 INIT 失败: " + e.getMessage());
        }

        // 6. 注册监听器并初始化新引擎
        newEngine.addListener(newListener);
        newEngine.init();

        System.out.println("[WebSocket] 尺寸切换: " + gameId + " -> " + sizeName.toUpperCase()
            + " (" + newConfig.gridWidth() + "×" + newConfig.gridHeight() + ")");
    }

    // ===== 内部方法 =====

    /**
     * 为指定会话创建游戏事件监听器。
     * 提取为工厂方法，供连接建立和尺寸切换复用。
     */
    private GameEventListener createGameEventListener(String gameId, WebSocketSession session, GameEngine engine) {
        return new GameEventListener() {
            @Override
            public void onStateUpdate(GameStateSnapshot snapshot) {
                try {
                    Map<String, Object> msg = Map.of(
                        "type", "STATE",
                        "gameId", gameId,
                        "snake", snapshot.snakeBody().stream()
                            .map(p -> new int[]{p.x(), p.y()}).toList(),
                        "food", new int[]{snapshot.foodPosition().x(), snapshot.foodPosition().y()},
                        "foodType", snapshot.foodType().name(),
                        "score", snapshot.score(),
                        "highScore", snapshot.highScore(),
                        "gameState", snapshot.gameState().name(),
                        "speedBoost", snapshot.speedBoost()
                    );
                    sendJson(session, msg);
                } catch (Exception e) {
                    System.err.println("推送状态失败: " + e.getMessage());
                }
            }

            @Override
            public void onEvent(GameEvent event) {
                try {
                    String action = event.name();
                    String messageText = switch (event) {
                        case FOOD_EATEN -> "吃到食物！";
                        case GAME_OVER -> "游戏结束！得分: " + engine.getScoreTracker().getCurrentScore();
                        case HIGH_SCORE -> "新最高分！";
                        case GAME_STARTED -> "游戏开始！";
                        case GAME_PAUSED -> "游戏暂停";
                        case GAME_RESUMED -> "游戏继续";
                    };

                    Map<String, Object> msg = Map.of(
                        "type", "EVENT",
                        "gameId", gameId,
                        "action", action,
                        "score", engine.getScoreTracker().getCurrentScore(),
                        "highScore", engine.getScoreTracker().getHighScore(),
                        "message", messageText
                    );
                    sendJson(session, msg);

                    // 游戏结束时自动保存分数
                    if (event == GameEvent.GAME_OVER && engine.getScoreTracker().getCurrentScore() > 0) {
                        try {
                            highScoreService.saveScore("玩家",
                                engine.getScoreTracker().getCurrentScore(),
                                engine.getSnake().getLength(),
                                engine.getFoodEatenCount());
                        } catch (Exception ex) {
                            System.err.println("保存分数失败: " + ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("推送事件失败: " + e.getMessage());
                }
            }
        };
    }

    private void sendJson(WebSocketSession session, Object data) throws IOException {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        }
    }
}
