package com.snakegame.web.dto;

/**
 * 服务端→客户端：事件通知消息。
 * 用于驱动前端音效和 UI 特效。
 */
public class EventMessage {
    private String type = "EVENT";
    private String gameId;
    private String action;   // "FOOD_EATEN" | "GAME_OVER" | "HIGH_SCORE" | "GAME_STARTED"
    private int score;
    private int highScore;
    private String message;

    public EventMessage() {}

    public static EventMessage of(String gameId, String action, int score, int highScore, String message) {
        EventMessage msg = new EventMessage();
        msg.gameId = gameId;
        msg.action = action;
        msg.score = score;
        msg.highScore = highScore;
        msg.message = message;
        return msg;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
