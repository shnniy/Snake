package com.snakegame.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 最高分文档。
 * 使用 Spring Data MongoDB 持久化到内嵌 MongoDB。
 */
@Document(collection = "high_scores")
public class HighScoreEntity {

    @Id
    private String id;

    private String playerName;
    private int score;
    private int snakeLength;
    private int foodEaten;
    private LocalDateTime createdAt;

    public HighScoreEntity() {
        this.createdAt = LocalDateTime.now();
    }

    public HighScoreEntity(String playerName, int score, int snakeLength, int foodEaten) {
        this.playerName = playerName;
        this.score = score;
        this.snakeLength = snakeLength;
        this.foodEaten = foodEaten;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getSnakeLength() { return snakeLength; }
    public void setSnakeLength(int snakeLength) { this.snakeLength = snakeLength; }
    public int getFoodEaten() { return foodEaten; }
    public void setFoodEaten(int foodEaten) { this.foodEaten = foodEaten; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
