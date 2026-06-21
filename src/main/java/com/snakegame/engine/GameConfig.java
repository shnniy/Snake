package com.snakegame.engine;

/**
 * 游戏配置。
 * 使用 Java record 实现不可变性，包含所有可调参数。
 *
 * @param gridWidth             网格宽度（默认 20）
 * @param gridHeight            网格高度（默认 15）
 * @param initialSnakeLength    蛇初始长度（默认 3）
 * @param normalTickMs          正常速度下每次 tick 的毫秒间隔（默认 100ms = 10fps）
 * @param fastTickMs            加速模式下每次 tick 的毫秒间隔（默认 50ms = 20fps）
 * @param foodScoreNormal       普通食物分数（默认 10）
 * @param foodScoreBonus        奖励食物分数（默认 30）
 * @param speedIncreaseThreshold 每吃 N 个食物后加速（第3期，默认 5）
 */
public record GameConfig(
    int gridWidth,
    int gridHeight,
    int initialSnakeLength,
    long normalTickMs,
    long fastTickMs,
    int foodScoreNormal,
    int foodScoreBonus,
    int speedIncreaseThreshold
) {
    /**
     * 紧凑构造函数，验证所有参数。
     */
    public GameConfig {
        if (gridWidth < 10 || gridHeight < 10) {
            throw new IllegalArgumentException("网格大小不能小于 10: " + gridWidth + "×" + gridHeight);
        }
        if (initialSnakeLength < 2) {
            throw new IllegalArgumentException("蛇初始长度不能小于 2: " + initialSnakeLength);
        }
        if (normalTickMs < 20 || normalTickMs > 1000) {
            throw new IllegalArgumentException("tick 间隔必须在 20-1000ms 之间: " + normalTickMs);
        }
        if (fastTickMs < 10 || fastTickMs >= normalTickMs) {
            throw new IllegalArgumentException("加速 tick 间隔必须小于正常间隔且在 10ms 以上: " + fastTickMs);
        }
        if (foodScoreNormal <= 0 || foodScoreBonus <= 0) {
            throw new IllegalArgumentException("食物分数必须大于 0");
        }
        if (speedIncreaseThreshold < 1) {
            throw new IllegalArgumentException("加速阈值必须大于 0");
        }
    }

    /**
     * 创建默认配置（20×15 网格，初始长度3，正常100ms，加速50ms）。
     */
    public static GameConfig defaultConfig() {
        // 正常移速 200ms/格，加速 100ms/格（为原 1/2 速度）
        return new GameConfig(20, 15, 3, 200, 100, 10, 30, 5);
    }
}
