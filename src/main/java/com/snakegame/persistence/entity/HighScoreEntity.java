package com.snakegame.persistence.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 最高分实体类。
 * 使用 JPA 持久化到 H2 数据库。
 */
@Entity
@Table(name = "high_scores")
public class HighScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String playerName;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private int snakeLength;

    @Column(nullable = false)
    private int foodEaten;

    @Column(nullable = false)
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
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
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
