package com.snakegame.engine;

/**
 * 游戏状态枚举。
 * 定义游戏生命周期的各个阶段。
 */
public enum GameState {
    /** 就绪状态：游戏已初始化，等待开始 */
    READY,

    /** 运行中：游戏循环正在执行 */
    RUNNING,

    /** 已暂停：游戏循环暂时停止 */
    PAUSED,

    /** 游戏结束：蛇发生碰撞，游戏终止 */
    GAME_OVER
}
