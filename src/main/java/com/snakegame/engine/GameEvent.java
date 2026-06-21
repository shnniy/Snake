package com.snakegame.engine;

/**
 * 游戏事件枚举。
 * 用于通知前端重要事件，驱动音效和 UI 更新。
 */
public enum GameEvent {
    /** 食物被吃掉 */
    FOOD_EATEN,

    /** 游戏结束 */
    GAME_OVER,

    /** 新最高分 */
    HIGH_SCORE,

    /** 游戏开始 */
    GAME_STARTED,

    /** 游戏暂停 */
    GAME_PAUSED,

    /** 游戏恢复 */
    GAME_RESUMED
}
