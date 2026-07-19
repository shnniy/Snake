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
     * 创建默认/标准配置（20×15 网格，初始长度3，正常200ms，加速100ms）。
     */
    public static GameConfig defaultConfig() {
        // 正常移速 200ms/格，加速 100ms/格（为原 1/2 速度）
        return new GameConfig(20, 15, 3, 200, 100, 10, 30, 5);
    }

    /**
     * 小窗口预设：15×10 网格 (3:2)。
     */
    public static GameConfig smallConfig() {
        return new GameConfig(15, 10, 3, 200, 100, 10, 30, 5);
    }

    /**
     * 标准窗口预设：20×15 网格 (4:3)，与 defaultConfig 相同。
     */
    public static GameConfig standardConfig() {
        return defaultConfig();
    }

    /**
     * 大窗口预设：30×20 网格 (3:2)。
     */
    public static GameConfig largeConfig() {
        return new GameConfig(30, 20, 3, 200, 100, 10, 30, 5);
    }

    /**
     * 根据名称获取预设配置。
     *
     * @param sizeName "SMALL" | "STANDARD" | "LARGE"（大小写不敏感）
     * @return 对应的 GameConfig，未知名称返回 standardConfig
     */
    public static GameConfig fromSizeName(String sizeName) {
        return switch (sizeName.toUpperCase()) {
            case "SMALL" -> smallConfig();
            case "LARGE" -> largeConfig();
            default -> standardConfig();
        };
    }
}
