package com.snakegame.engine;

/**
 * 游戏事件监听器接口（观察者模式）。
 * 由 GameSessionManager 实现，用于 WebSocket 推送游戏状态。
 */
public interface GameEventListener {

    /**
     * 当游戏状态更新时调用（每个 tick）。
     *
     * @param snapshot 当前游戏状态快照
     */
    void onStateUpdate(GameStateSnapshot snapshot);

    /**
     * 当重要游戏事件发生时调用。
     *
     * @param event 游戏事件
     */
    void onEvent(GameEvent event);
}
