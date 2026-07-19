package com.snakegame.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GameConfig 配置测试")
class GameConfigTest {

    @Test
    @DisplayName("默认配置应包含正确的值")
    void defaultConfig() {
        GameConfig config = GameConfig.defaultConfig();
        assertEquals(20, config.gridWidth());
        assertEquals(15, config.gridHeight());
        assertEquals(3, config.initialSnakeLength());
        assertEquals(200, config.normalTickMs());
        assertEquals(100, config.fastTickMs());
        assertEquals(10, config.foodScoreNormal());
        assertEquals(30, config.foodScoreBonus());
        assertEquals(5, config.speedIncreaseThreshold());
    }

    @Test
    @DisplayName("有效的自定义配置应创建成功")
    void validCustomConfig() {
        GameConfig config = new GameConfig(30, 20, 4, 150, 75, 15, 50, 3);
        assertEquals(30, config.gridWidth());
        assertEquals(20, config.gridHeight());
    }

    @Test
    @DisplayName("网格太小应抛出异常")
    void gridTooSmall() {
        assertThrows(IllegalArgumentException.class, () ->
            new GameConfig(5, 10, 3, 100, 50, 10, 30, 5));
    }

    @Test
    @DisplayName("蛇初始长度太小应抛出异常")
    void snakeTooShort() {
        assertThrows(IllegalArgumentException.class, () ->
            new GameConfig(20, 15, 1, 100, 50, 10, 30, 5));
    }

    @Test
    @DisplayName("加速 tick 不能大于等于正常 tick")
    void fastTickNotLessThanNormal() {
        assertThrows(IllegalArgumentException.class, () ->
            new GameConfig(20, 15, 3, 100, 100, 10, 30, 5));
        assertThrows(IllegalArgumentException.class, () ->
            new GameConfig(20, 15, 3, 100, 150, 10, 30, 5));
    }

    @Test
    @DisplayName("食物分数不能为负数")
    void foodScoreNotNegative() {
        assertThrows(IllegalArgumentException.class, () ->
            new GameConfig(20, 15, 3, 100, 50, 0, 30, 5));
    }

    @Test
    @DisplayName("加速阈值必须大于 0")
    void speedThresholdPositive() {
        assertThrows(IllegalArgumentException.class, () ->
            new GameConfig(20, 15, 3, 100, 50, 10, 30, 0));
    }

    // ===== 预设尺寸测试 =====

    @Test
    @DisplayName("fromSizeName SMALL 应返回 15×10")
    void fromSizeNameSmall() {
        GameConfig config = GameConfig.fromSizeName("SMALL");
        assertEquals(15, config.gridWidth());
        assertEquals(10, config.gridHeight());
    }

    @Test
    @DisplayName("fromSizeName STANDARD 应返回 20×15")
    void fromSizeNameStandard() {
        GameConfig config = GameConfig.fromSizeName("STANDARD");
        assertEquals(20, config.gridWidth());
        assertEquals(15, config.gridHeight());
    }

    @Test
    @DisplayName("fromSizeName LARGE 应返回 30×20")
    void fromSizeNameLarge() {
        GameConfig config = GameConfig.fromSizeName("LARGE");
        assertEquals(30, config.gridWidth());
        assertEquals(20, config.gridHeight());
    }

    @Test
    @DisplayName("fromSizeName 未知名称应回退到标准配置")
    void fromSizeNameUnknownReturnsStandard() {
        GameConfig config = GameConfig.fromSizeName("INVALID");
        assertEquals(20, config.gridWidth());
        assertEquals(15, config.gridHeight());
    }

    @Test
    @DisplayName("fromSizeName 应大小写不敏感")
    void fromSizeNameCaseInsensitive() {
        assertEquals(15, GameConfig.fromSizeName("small").gridWidth());
        assertEquals(20, GameConfig.fromSizeName("Standard").gridWidth());
        assertEquals(30, GameConfig.fromSizeName("Large").gridWidth());
    }

    @Test
    @DisplayName("smallConfig 应返回正确的小窗口配置")
    void smallConfigCorrect() {
        GameConfig config = GameConfig.smallConfig();
        assertEquals(15, config.gridWidth());
        assertEquals(10, config.gridHeight());
        assertEquals(3, config.initialSnakeLength());
        assertEquals(200, config.normalTickMs());
    }

    @Test
    @DisplayName("largeConfig 应返回正确的大窗口配置")
    void largeConfigCorrect() {
        GameConfig config = GameConfig.largeConfig();
        assertEquals(30, config.gridWidth());
        assertEquals(20, config.gridHeight());
        assertEquals(3, config.initialSnakeLength());
        assertEquals(200, config.normalTickMs());
    }
}
