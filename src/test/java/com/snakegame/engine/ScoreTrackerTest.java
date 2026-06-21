package com.snakegame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ScoreTracker 分数追踪器测试")
class ScoreTrackerTest {
    private ScoreTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ScoreTracker();
    }

    @Test
    @DisplayName("初始分数应为 0")
    void initialScoreIsZero() {
        assertEquals(0, tracker.getCurrentScore());
        assertEquals(0, tracker.getHighScore());
    }

    @Test
    @DisplayName("增加分数应正确累加")
    void addScore() {
        tracker.addScore(10);
        assertEquals(10, tracker.getCurrentScore());
        tracker.addScore(30);
        assertEquals(40, tracker.getCurrentScore());
    }

    @Test
    @DisplayName("当前分数超过最高分时应更新最高分")
    void highScoreUpdated() {
        tracker.addScore(50);
        assertEquals(50, tracker.getHighScore());
        tracker.addScore(30);
        assertEquals(80, tracker.getCurrentScore());
        assertEquals(80, tracker.getHighScore());
    }

    @Test
    @DisplayName("重置当前分数应保留最高分")
    void resetPreservesHighScore() {
        tracker.addScore(100);
        assertEquals(100, tracker.getHighScore());
        tracker.reset();
        assertEquals(0, tracker.getCurrentScore());
        assertEquals(100, tracker.getHighScore());
    }

    @Test
    @DisplayName("完全重置应清除所有分数")
    void resetAllClearsEverything() {
        tracker.addScore(200);
        tracker.resetAll();
        assertEquals(0, tracker.getCurrentScore());
        assertEquals(0, tracker.getHighScore());
    }

    @Test
    @DisplayName("设置最高分")
    void setHighScore() {
        tracker.setHighScore(500);
        assertEquals(500, tracker.getHighScore());
    }

    @Test
    @DisplayName("负数分数应抛出异常")
    void negativeScoreThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> tracker.addScore(-10));
    }
}
