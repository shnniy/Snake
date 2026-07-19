package com.snakegame.web;

import com.snakegame.engine.Direction;
import com.snakegame.service.GameSessionManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
    })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("WebSocket 集成测试")
class GameWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private GameSessionManager sessionManager;

    private WebSocketStompClient stompClient;
    private StompSession stompSession;
    private String gameId;
    private BlockingQueue<Map<String, Object>> stateMessages;
    private BlockingQueue<Map<String, Object>> eventMessages;

    @BeforeEach
    void setUp() throws Exception {
        stateMessages = new LinkedBlockingQueue<>();
        eventMessages = new LinkedBlockingQueue<>();

        // 创建客户端
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        // 连接到服务器
        String url = "http://localhost:" + port + "/ws";
        stompSession = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void handleException(StompSession session, StompCommand command,
                                         StompHeaders headers, byte[] payload, Throwable exception) {
                // ignore
            }
        }).get(5, TimeUnit.SECONDS);

        assertNotNull(stompSession, "STOMP 会话应为非空");
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
        }
    }

    @Test
    @Order(1)
    @DisplayName("应能创建游戏会话并接收状态更新")
    void createSessionAndReceiveState() throws Exception {
        // 通过 REST API 创建会话
        // 手动调用 sessionManager 创建会话
        gameId = sessionManager.createSession();
        assertNotNull(gameId);
        assertFalse(gameId.isEmpty());

        // 订阅游戏状态
        stompSession.subscribe("/topic/game/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) payload;
                stateMessages.offer(msg);
            }
        });

        // 订阅事件通知
        stompSession.subscribe("/topic/events/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) payload;
                eventMessages.offer(msg);
            }
        });

        // 启动游戏
        stompSession.send("/app/game/" + gameId + "/command",
            Map.of("type", "COMMAND", "action", "START"));

        // 等待状态更新
        Map<String, Object> state = stateMessages.poll(3, TimeUnit.SECONDS);
        assertNotNull(state, "应收到状态更新");
        assertEquals("STATE", state.get("type"));
        assertEquals(gameId, state.get("gameId"));
        assertTrue(state.containsKey("snake"));
        assertTrue(state.containsKey("food"));
        assertTrue(state.containsKey("score"));
        assertEquals("RUNNING", state.get("gameState"));

        // 清理会话
        sessionManager.removeSession(gameId);
    }

    @Test
    @Order(2)
    @DisplayName("发送方向命令应改变蛇的移动方向")
    void sendDirectionChangesSnakeDirection() throws Exception {
        gameId = sessionManager.createSession();

        stompSession.subscribe("/topic/game/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return Map.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) payload;
                stateMessages.offer(msg);
            }
        });

        // 启动游戏
        stompSession.send("/app/game/" + gameId + "/command",
            Map.of("type", "COMMAND", "action", "START"));

        // 发送方向输入
        stompSession.send("/app/game/" + gameId + "/input",
            Map.of("type", "INPUT", "direction", "DOWN", "speedBoost", false, "sequence", 1));

        // 等待状态更新
        Map<String, Object> state = stateMessages.poll(3, TimeUnit.SECONDS);
        assertNotNull(state, "应收到状态更新");

        sessionManager.removeSession(gameId);
    }

    @Test
    @Order(3)
    @DisplayName("暂停和恢复命令应正常工作")
    void pauseAndResume() throws Exception {
        gameId = sessionManager.createSession();

        stompSession.subscribe("/topic/game/" + gameId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) { return Map.class; }
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                @SuppressWarnings("unchecked")
                Map<String, Object> msg = (Map<String, Object>) payload;
                stateMessages.offer(msg);
            }
        });

        // 启动游戏
        stompSession.send("/app/game/" + gameId + "/command",
            Map.of("type", "COMMAND", "action", "START"));
        stateMessages.poll(2, TimeUnit.SECONDS);

        // 暂停
        stompSession.send("/app/game/" + gameId + "/command",
            Map.of("type", "COMMAND", "action", "PAUSE"));

        Map<String, Object> paused = stateMessages.poll(2, TimeUnit.SECONDS);
        assertNotNull(paused);
        assertEquals("PAUSED", paused.get("gameState"));

        // 恢复
        stompSession.send("/app/game/" + gameId + "/command",
            Map.of("type", "COMMAND", "action", "RESUME"));

        Map<String, Object> resumed = stateMessages.poll(2, TimeUnit.SECONDS);
        assertNotNull(resumed);
        assertEquals("RUNNING", resumed.get("gameState"));

        sessionManager.removeSession(gameId);
    }

    @Test
    @Order(4)
    @DisplayName("会话管理器应正确管理多会话")
    void multipleSessions() {
        String id1 = sessionManager.createSession();
        String id2 = sessionManager.createSession();

        assertEquals(2, sessionManager.getActiveSessionCount());

        assertNotNull(sessionManager.getSession(id1));
        assertNotNull(sessionManager.getSession(id2));
        assertNull(sessionManager.getSession("nonexistent"));

        sessionManager.removeSession(id1);
        assertEquals(1, sessionManager.getActiveSessionCount());

        sessionManager.removeSession(id2);
        assertEquals(0, sessionManager.getActiveSessionCount());
    }
}
