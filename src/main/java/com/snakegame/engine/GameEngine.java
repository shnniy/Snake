package com.snakegame.engine;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 游戏引擎 — 贪吃蛇游戏的核心控制器。
 *
 * 使用单线程 ScheduledExecutorService 驱动游戏循环，
 * tick() 是唯一的状态修改方法，保证线程安全。
 * 用户输入通过 volatile 字段在 tick 开始时读取。
 */
public class GameEngine {
    private final Grid grid;
    private final Snake snake;
    private final Food food;
    private final GameConfig config;
    private final ScoreTracker scoreTracker;
    private GameState state;

    // 用户输入（线程安全队列 — 支持快速连续方向输入）
    private final ConcurrentLinkedQueue<Direction> directionQueue;
    private volatile boolean speedBoost;

    // 游戏循环
    private ScheduledExecutorService gameLoopExecutor;
    private ScheduledFuture<?> tickFuture;

    // 观察者
    private final List<GameEventListener> listeners;

    // 游戏统计
    private int foodEatenCount;

    /**
     * 使用指定配置创建游戏引擎。
     */
    public GameEngine(GameConfig config) {
        this.config = config;
        this.grid = new Grid(config.gridWidth(), config.gridHeight());
        this.food = new Food();
        this.scoreTracker = new ScoreTracker();
        this.listeners = new CopyOnWriteArrayList<>();
        this.directionQueue = new ConcurrentLinkedQueue<>();
        this.state = GameState.READY;

        // 计算蛇头初始位置（网格中央偏左）
        Position startHead = new Position(config.gridWidth() / 4, config.gridHeight() / 2);
        this.snake = new Snake(startHead, config.initialSnakeLength(), Direction.RIGHT);
    }

    /**
     * 初始化游戏：重置蛇位置、放置食物，将状态设为 READY。
     */
    public void init() {
        stopGameLoop();
        this.state = GameState.READY;
        this.speedBoost = false;
        this.foodEatenCount = 0;
        this.directionQueue.clear();

        scoreTracker.reset();

        // 重新生成蛇到起始位置
        Position startHead = new Position(config.gridWidth() / 4, config.gridHeight() / 2);
        reinitSnake(startHead);

        // 放置食物
        food.spawn(snake.occupiedCells(), grid);

        // 通知观察者
        notifyStateUpdate();
    }

    /**
     * 启动游戏循环，游戏从 READY 或 PAUSED/GAME_OVER 状态转换为 RUNNING。
     * GAME_OVER 状态下会自动先重置再启动。
     */
    public synchronized void start() {
        if (state == GameState.RUNNING) {
            return;
        }
        // GAME_OVER 状态下自动重置
        if (state == GameState.GAME_OVER) {
            stopGameLoop();
            scoreTracker.reset();
            Position startHead = new Position(config.gridWidth() / 4, config.gridHeight() / 2);
            reinitSnake(startHead);
            food.spawn(snake.occupiedCells(), grid);
        }
        this.state = GameState.RUNNING;
        startGameLoop();
        notifyEvent(GameEvent.GAME_STARTED);
        notifyStateUpdate();
    }

    /**
     * 暂停游戏。
     */
    public synchronized void pause() {
        if (state != GameState.RUNNING) {
            return;
        }
        this.state = GameState.PAUSED;
        stopGameLoop();
        notifyEvent(GameEvent.GAME_PAUSED);
        notifyStateUpdate();
    }

    /**
     * 恢复游戏（从暂停中）。
     */
    public synchronized void resume() {
        if (state != GameState.PAUSED) {
            return;
        }
        start();
        notifyEvent(GameEvent.GAME_RESUMED);
    }

    /**
     * 完全重置游戏。
     */
    public synchronized void reset() {
        stopGameLoop();
        this.state = GameState.READY;
        this.speedBoost = false;
        this.foodEatenCount = 0;
        this.directionQueue.clear();

        scoreTracker.reset();

        // 重建蛇
        Position startHead = new Position(config.gridWidth() / 4, config.gridHeight() / 2);
        // 使用反射重建私有字段不方便，这里重置蛇的方式是通过重新创建
        // 我们通过 init() 风格重置
        reinitSnake(startHead);

        food.spawn(snake.occupiedCells(), grid);
        notifyStateUpdate();
    }

    // ===== 用户输入 =====

    /**
     * 设置蛇的移动方向（添加到队列，在 tick 时处理）。
     * 支持快速连续输入，队列最多保留最近 3 个方向。
     */
    public void setDirection(Direction dir) {
        directionQueue.offer(dir);
        // 限制队列长度，避免处理过时的方向
        while (directionQueue.size() > 3) {
            directionQueue.poll();
        }
    }

    /**
     * 启用/禁用加速模式（Shift 键）。
     * 加速时 tick 间隔从 normalTickMs 切换到 fastTickMs。
     */
    public void setSpeedBoost(boolean enabled) {
        if (this.speedBoost != enabled) {
            this.speedBoost = enabled;
            // 如果正在运行，重新调度 tick
            if (state == GameState.RUNNING) {
                rescheduleTick();
            }
        }
    }

    // ===== 游戏循环 =====

    /**
     * 启动游戏循环。
     */
    private void startGameLoop() {
        if (gameLoopExecutor == null || gameLoopExecutor.isShutdown()) {
            gameLoopExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "snake-game-loop");
                t.setDaemon(true);
                return t;
            });
        }
        scheduleTick();
    }

    /**
     * 停止游戏循环。
     */
    private void stopGameLoop() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
            tickFuture = null;
        }
        if (gameLoopExecutor != null && !gameLoopExecutor.isShutdown()) {
            // 使用 shutdown() 而非 shutdownNow()，避免中断当前线程
            // （gameOver() 在 tick 线程内调用，中断会导致后续 WebSocket 推送失败）
            gameLoopExecutor.shutdown();
            gameLoopExecutor = null;
        }
    }

    /**
     * 调度下一次 tick。
     */
    private void scheduleTick() {
        if (gameLoopExecutor == null || gameLoopExecutor.isShutdown()) {
            return;
        }
        long delay = speedBoost ? config.fastTickMs() : config.normalTickMs();
        try {
            tickFuture = gameLoopExecutor.schedule(this::tick, delay, TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // 执行器已被关闭（可能在检查后由另一线程关闭），忽略
        }
    }

    /**
     * 重新调度 tick（用于加速/减速切换）。
     */
    private void rescheduleTick() {
        if (tickFuture != null) {
            tickFuture.cancel(false);
        }
        scheduleTick();
    }

    /**
     * 游戏循环的核心方法——每次 tick 执行一次。
     * 这是唯一修改游戏状态的方法，保证线程安全。
     */
    private void tick() {
        if (state != GameState.RUNNING) {
            return;
        }

        try {
            // 1. 处理输入：从队列中取最后一个有效方向
            Direction bestDir = null;
            Direction dir;
            while ((dir = directionQueue.poll()) != null) {
                // 取最后一个不与当前方向相反的方向
                if (!snake.getDirection().isOpposite(dir)) {
                    bestDir = dir;
                }
            }
            if (bestDir != null) {
                snake.setDirection(bestDir);
            }

            // 2. 计算下一位置
            Position nextHead = snake.peekNextHead();

            // 3. 墙壁碰撞检测
            if (!grid.isWithinBounds(nextHead)) {
                gameOver("撞到墙壁");
                return;
            }

            // 4. 自身碰撞检测
            if (snake.isSelfCollidingAt(nextHead)) {
                gameOver("撞到自己");
                return;
            }

            // 5. 食物碰撞检测
            boolean ateFood = false;
            if (nextHead.equals(food.getPosition())) {
                FoodType foodType = food.getType();
                snake.grow(foodType.growBy);
                scoreTracker.addScore(foodType.scoreValue);
                foodEatenCount++;
                ateFood = true;

                // 放置新食物
                Optional<Position> newFood = food.spawn(snake.occupiedCells(), grid);
                if (newFood.isEmpty()) {
                    // 蛇占满整格 — 玩家获胜!
                    gameOver("恭喜获胜！蛇已填满整个网格");
                    return;
                }

                notifyEvent(GameEvent.FOOD_EATEN);
            }

            // 6. 提交移动
            snake.move();

            // 7. 如果吃了食物，再次检查自身碰撞（增长后可能撞到自己）
            if (ateFood && snake.isSelfColliding()) {
                gameOver("撞到自己");
                return;
            }

            // 8. 通知观察者当前状态
            notifyStateUpdate();

            // 9. 调度下一次 tick
            scheduleTick();

        } catch (Exception e) {
            // 异常保护：记录并停止游戏
            System.err.println("游戏循环异常: " + e.getMessage());
            e.printStackTrace();
            state = GameState.GAME_OVER;
            notifyStateUpdate();
        }
    }

    /**
     * 处理游戏结束。
     */
    private void gameOver(String reason) {
        this.state = GameState.GAME_OVER;
        stopGameLoop();

        // 检查是否新高分
        boolean isHighScore = scoreTracker.getCurrentScore() >= scoreTracker.getHighScore()
                            && scoreTracker.getCurrentScore() > 0;

        System.out.println("游戏结束: " + reason + " | 得分: " + scoreTracker.getCurrentScore()
                         + " | 最高分: " + scoreTracker.getHighScore());

        notifyEvent(GameEvent.GAME_OVER);
        if (isHighScore) {
            notifyEvent(GameEvent.HIGH_SCORE);
        }
        notifyStateUpdate();
    }

    // ===== 观察者模式 =====

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(GameEventListener listener) {
        listeners.remove(listener);
    }

    private void notifyStateUpdate() {
        GameStateSnapshot snapshot = GameStateSnapshot.from(
            snake, food, scoreTracker, state, speedBoost);
        for (GameEventListener listener : listeners) {
            try {
                listener.onStateUpdate(snapshot);
            } catch (Exception e) {
                System.err.println("通知监听器异常: " + e.getMessage());
            }
        }
    }

    private void notifyEvent(GameEvent event) {
        for (GameEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                System.err.println("通知事件异常: " + e.getMessage());
            }
        }
    }

    // ===== 辅助方法 =====

    /**
     * 重新初始化蛇（用于重置）。
     */
    private void reinitSnake(Position startHead) {
        snake.reset(startHead, config.initialSnakeLength(), Direction.RIGHT);
    }

    // ===== 访问器 =====

    public GameState getState() {
        return state;
    }

    public Grid getGrid() {
        return grid;
    }

    public Snake getSnake() {
        return snake;
    }

    public Food getFood() {
        return food;
    }

    public ScoreTracker getScoreTracker() {
        return scoreTracker;
    }

    public GameConfig getConfig() {
        return config;
    }

    public boolean isSpeedBoost() {
        return speedBoost;
    }

    public int getFoodEatenCount() {
        return foodEatenCount;
    }

    /**
     * 获取当前 tick 间隔（考虑加速状态）。
     */
    public long getCurrentTickMs() {
        return speedBoost ? config.fastTickMs() : config.normalTickMs();
    }

    /**
     * 关闭引擎，释放资源。
     */
    public void shutdown() {
        stopGameLoop();
        listeners.clear();
    }
}
