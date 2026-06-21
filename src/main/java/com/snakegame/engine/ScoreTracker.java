package com.snakegame.engine;

/**
 * 分数追踪器。
 * 维护当前分数和最高分数（内存中）。
 */
public class ScoreTracker {
    private int currentScore;
    private int highScore;

    public ScoreTracker() {
        this.currentScore = 0;
        this.highScore = 0;
    }

    /**
     * 增加当前分数，如果超过最高分则更新最高分。
     *
     * @param points 增加的分数
     */
    public void addScore(int points) {
        if (points < 0) {
            throw new IllegalArgumentException("分数不能为负数: " + points);
        }
        currentScore += points;
        if (currentScore > highScore) {
            highScore = currentScore;
        }
    }

    /**
     * 重置当前分数（保留最高分）。
     */
    public void reset() {
        currentScore = 0;
    }

    /**
     * 完全重置（包括最高分）。
     */
    public void resetAll() {
        currentScore = 0;
        highScore = 0;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public int getHighScore() {
        return highScore;
    }

    /** 设置最高分（从持久化加载时使用） */
    public void setHighScore(int highScore) {
        if (highScore >= 0) {
            this.highScore = highScore;
        }
    }

    @Override
    public String toString() {
        return "ScoreTracker{current=" + currentScore + ", high=" + highScore + "}";
    }
}
