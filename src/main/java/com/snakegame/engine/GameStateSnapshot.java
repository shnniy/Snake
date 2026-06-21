package com.snakegame.engine;

import java.util.List;

/**
 * 游戏状态快照（不可变 DTO）。
 * 每个 tick 生成一次，通过 WebSocket 推送给前端。
 *
 * @param snakeBody   蛇身所有节的位置列表（头部为第一个元素）
 * @param foodPosition 食物的位置
 * @param foodType    食物类型
 * @param score       当前分数
 * @param highScore   最高分数
 * @param gameState   游戏状态
 * @param speedBoost  是否处于加速模式
 * @param timestamp   快照时间戳（毫秒）
 */
public record GameStateSnapshot(
    List<Position> snakeBody,
    Position foodPosition,
    FoodType foodType,
    int score,
    int highScore,
    GameState gameState,
    boolean speedBoost,
    long timestamp
) {
    /**
     * 从 GameEngine 当前状态创建快照。
     */
    public static GameStateSnapshot from(
            Snake snake, Food food, ScoreTracker scoreTracker,
            GameState state, boolean speedBoost) {
        return new GameStateSnapshot(
            snake.getBody(),
            food.getPosition(),
            food.getType(),
            scoreTracker.getCurrentScore(),
            scoreTracker.getHighScore(),
            state,
            speedBoost,
            System.currentTimeMillis()
        );
    }
}
