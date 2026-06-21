package com.snakegame.web.dto;

import com.snakegame.engine.FoodType;
import com.snakegame.engine.GameState;
import com.snakegame.engine.Position;

import java.util.List;

/**
 * 服务端→客户端：游戏状态更新消息。
 * 每个 tick 推送一次，包含完整的游戏状态用于前端渲染。
 */
public class GameStateMessage {
    private String type = "STATE";
    private String gameId;
    private List<int[]> snake;       // [[x,y], [x,y], ...]
    private int[] food;              // [x, y]
    private String foodType;         // "NORMAL" | "BONUS"
    private int score;
    private int highScore;
    private String gameState;        // "READY" | "RUNNING" | "PAUSED" | "GAME_OVER"
    private boolean speedBoost;
    private long timestamp;

    public GameStateMessage() {}

    public static GameStateMessage from(String gameId, List<Position> snakeBody,
                                         Position foodPos, FoodType foodType,
                                         int score, int highScore,
                                         GameState gameState, boolean speedBoost) {
        GameStateMessage msg = new GameStateMessage();
        msg.gameId = gameId;
        msg.snake = snakeBody.stream()
                .map(p -> new int[]{p.x(), p.y()})
                .toList();
        msg.food = new int[]{foodPos.x(), foodPos.y()};
        msg.foodType = foodType.name();
        msg.score = score;
        msg.highScore = highScore;
        msg.gameState = gameState.name();
        msg.speedBoost = speedBoost;
        msg.timestamp = System.currentTimeMillis();
        return msg;
    }

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public List<int[]> getSnake() { return snake; }
    public void setSnake(List<int[]> snake) { this.snake = snake; }
    public int[] getFood() { return food; }
    public void setFood(int[] food) { this.food = food; }
    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getHighScore() { return highScore; }
    public void setHighScore(int highScore) { this.highScore = highScore; }
    public String getGameState() { return gameState; }
    public void setGameState(String gameState) { this.gameState = gameState; }
    public boolean isSpeedBoost() { return speedBoost; }
    public void setSpeedBoost(boolean speedBoost) { this.speedBoost = speedBoost; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
