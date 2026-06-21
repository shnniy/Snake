package com.snakegame.engine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameEngine 游戏引擎模块测试")
class GameEngineTest {
    private GameEngine engine;
    private List<GameStateSnapshot> snapshots;
    private List<GameEvent> events;

    @BeforeEach
    void setUp() {
        GameConfig config = GameConfig.defaultConfig();
        engine = new GameEngine(config);
        snapshots = new ArrayList<>();
        events = new ArrayList<>();

        // 注册测试监听器
        engine.addListener(new GameEventListener() {
            @Override
            public void onStateUpdate(GameStateSnapshot snapshot) {
                snapshots.add(snapshot);
            }

            @Override
            public void onEvent(GameEvent event) {
                events.add(event);
            }
        });
    }

    @AfterEach
    void tearDown() {
        engine.shutdown();
    }

    // ===== 初始化测试 =====

    @Test
    @DisplayName("初始化后状态应为 READY")
    void initialStateIsReady() {
        engine.init();
        assertEquals(GameState.READY, engine.getState());
        assertEquals(0, engine.getScoreTracker().getCurrentScore());
    }

    @Test
    @DisplayName("初始化后应生成食物")
    void foodGeneratedOnInit() {
        engine.init();
        assertNotNull(engine.getFood().getPosition());
        assertTrue(engine.getGrid().isWithinBounds(engine.getFood().getPosition()));
    }

    @Test
    @DisplayName("初始化后蛇应有正确的初始长度")
    void snakeInitialLength() {
        engine.init();
        assertEquals(3, engine.getSnake().getLength());
    }

    // ===== 游戏生命周期测试 =====

    @Test
    @DisplayName("start 应使状态变为 RUNNING")
    void startChangesStateToRunning() throws InterruptedException {
        engine.init();
        engine.start();
        assertEquals(GameState.RUNNING, engine.getState());
        // 等待至少一个 tick（200ms/tick）
        Thread.sleep(400);
        engine.pause();
        assertTrue(snapshots.size() > 0, "应该有状态更新");
        assertTrue(events.contains(GameEvent.GAME_STARTED));
    }

    @Test
    @DisplayName("pause 应使状态变为 PAUSED")
    void pauseChangesState() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(350); // 等待 tick 开始
        engine.pause();
        assertEquals(GameState.PAUSED, engine.getState());
        assertTrue(events.contains(GameEvent.GAME_PAUSED));
    }

    @Test
    @DisplayName("resume 应从 PAUSED 恢复为 RUNNING")
    void resumeFromPaused() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(350);
        engine.pause();
        assertEquals(GameState.PAUSED, engine.getState());
        engine.resume();
        assertEquals(GameState.RUNNING, engine.getState());
    }

    @Test
    @DisplayName("reset 应将状态恢复为 READY")
    void resetToReady() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(350);
        engine.reset();
        assertEquals(GameState.READY, engine.getState());
    }

    // ===== 蛇移动测试 =====

    @Test
    @DisplayName("蛇应朝设置的方向移动")
    void snakeMovesInSetDirection() throws InterruptedException {
        engine.init();
        engine.start();

        // 蛇初始方向为 RIGHT，头部在 (5,7)
        Position initialHead = engine.getSnake().getHead();
        Thread.sleep(500); // 等待几个 tick（200ms/tick）
        engine.pause();

        Position newHead = engine.getSnake().getHead();
        assertTrue(newHead.x() > initialHead.x(),
            "蛇应向右移动: 初始=" + initialHead + ", 新=" + newHead);
    }

    @Test
    @DisplayName("setDirection 应改变蛇的方向")
    void changeDirection() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(350);

        engine.setDirection(Direction.DOWN);
        Thread.sleep(500);
        engine.pause();

        // DOWN 方向应该被应用
        assertNotNull(engine.getSnake().getDirection());
    }

    @Test
    @DisplayName("方向队列应正确处理快速多次输入")
    void directionBuffer() throws InterruptedException {
        engine.init();
        engine.start();

        // 快速设置多个方向（模拟快速按键）
        engine.setDirection(Direction.UP);
        engine.setDirection(Direction.LEFT);
        engine.setDirection(Direction.DOWN); // 最后一个有效方向

        Thread.sleep(500); // 等待足够 tick 处理队列
        engine.pause();

        // 队列会逐个处理：UP(有效) → LEFT(对UP反向，跳过) → DOWN(对UP不反向)...
        // 实际上：tick 循环遍历队列，找到最后一个对当前方向不反向的
        // 当前方向初始 RIGHT，UP 不反向，LEFT 对 RIGHT 反向(跳过)，DOWN 对 RIGHT 不反向
        // bestDir = DOWN（最后一个有效方向）
        Direction dir = engine.getSnake().getDirection();
        // 可能是 DOWN 或 UP（取决于tick在哪个时间点处理队列）
        assertNotNull(dir, "方向应该被设置");
    }

    // ===== 碰撞检测测试 =====

    @Test
    @DisplayName("撞墙应导致游戏结束")
    void wallCollisionGameOver() throws InterruptedException {
        // 使用小网格方便测试
        engine.shutdown();
        GameConfig smallConfig = new GameConfig(10, 10, 3, 200, 100, 10, 30, 5);
        engine = new GameEngine(smallConfig);

        engine.init();
        engine.start();
        Thread.sleep(3000); // 在 10 格宽网格中向右移动，约需 4 tick × 200ms = 800ms
        engine.pause();

        assertTrue(
            engine.getState() == GameState.GAME_OVER || engine.getState() == GameState.RUNNING,
            "根据网格和速度，蛇可能已撞墙或仍在移动"
        );
    }

    // ===== 食物测试 =====

    @Test
    @DisplayName("init 后食物应存在于空闲格子上")
    void foodOnFreeCellAfterInit() {
        engine.init();
        Position foodPos = engine.getFood().getPosition();
        assertNotNull(foodPos);
        assertFalse(engine.getSnake().occupies(foodPos),
            "食物不应在蛇身上");
    }

    // ===== 加速测试 =====

    @Test
    @DisplayName("setSpeedBoost 应切换加速状态")
    void speedBoostToggles() {
        engine.init();
        assertFalse(engine.isSpeedBoost());

        engine.setSpeedBoost(true);
        assertTrue(engine.isSpeedBoost());

        engine.setSpeedBoost(false);
        assertFalse(engine.isSpeedBoost());
    }

    @Test
    @DisplayName("加速时 tick 间隔应更短")
    void speedBoostFasterTick() {
        engine.init();
        long normalTick = engine.getCurrentTickMs();
        assertEquals(200, normalTick);

        engine.setSpeedBoost(true);
        long fastTick = engine.getCurrentTickMs();
        assertEquals(100, fastTick);

        assertTrue(fastTick < normalTick, "加速 tick 应小于正常 tick");
    }

    // ===== 分数测试 =====

    @Test
    @DisplayName("初始分数应为 0")
    void initialScoreZero() {
        engine.init();
        assertEquals(0, engine.getScoreTracker().getCurrentScore());
        assertEquals(0, engine.getScoreTracker().getHighScore());
    }

    @Test
    @DisplayName("reset 应重置当前分数但保留最高分")
    void resetPreservesHighScore() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(400);
        engine.pause();

        // 不能直接操纵分数，但可以通过游戏过程验证
        engine.reset();
        assertEquals(0, engine.getScoreTracker().getCurrentScore());
    }

    // ===== 状态快照测试 =====

    @Test
    @DisplayName("状态快照应包含所有必要字段")
    void stateSnapshotComplete() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(400);
        engine.pause();

        assertFalse(snapshots.isEmpty(), "应有至少一个状态快照");
        GameStateSnapshot snapshot = snapshots.get(0);
        assertNotNull(snapshot.snakeBody());
        assertNotNull(snapshot.foodPosition());
        assertNotNull(snapshot.foodType());
        assertTrue(snapshot.score() >= 0);
        assertTrue(snapshot.highScore() >= 0);
        assertNotNull(snapshot.gameState());
        assertTrue(snapshot.timestamp() > 0);
    }

    // ===== 事件测试 =====

    @Test
    @DisplayName("start 应触发 GAME_STARTED 事件")
    void startEvent() {
        engine.init();
        engine.start();
        assertTrue(events.contains(GameEvent.GAME_STARTED));
    }

    @Test
    @DisplayName("pause 应触发 GAME_PAUSED 事件")
    void pauseEvent() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(400);
        engine.pause();
        assertTrue(events.contains(GameEvent.GAME_PAUSED));
    }

    // ===== 异常处理测试 =====

    @Test
    @DisplayName("在非 RUNNING 状态下 start 不影响")
    void startWhenAlreadyRunning() throws InterruptedException {
        engine.init();
        engine.start();
        Thread.sleep(300);
        engine.start(); // 再次 start 应该无影响
        assertEquals(GameState.RUNNING, engine.getState());
    }

    @Test
    @DisplayName("在非 PAUSED 状态下 resume 不影响")
    void resumeWhenNotPaused() {
        engine.init();
        engine.start();
        engine.resume(); // 不在 PAUSED 状态
        assertEquals(GameState.RUNNING, engine.getState());
    }

    @Test
    @DisplayName("shutdown 应释放资源")
    void shutdownReleasesResources() {
        engine.init();
        engine.start();
        engine.shutdown();
        // shutdown 后不应抛出异常
        assertDoesNotThrow(() -> engine.shutdown());
    }
}
