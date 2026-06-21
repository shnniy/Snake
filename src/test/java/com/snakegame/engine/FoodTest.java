package com.snakegame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Food 食物测试")
class FoodTest {
    private Food food;
    private Grid grid;

    @BeforeEach
    void setUp() {
        food = new Food();
        grid = new Grid(20, 15);
    }

    @Test
    @DisplayName("spawn 应在空闲格子中放置食物")
    void spawnOnFreeCell() {
        Set<Position> occupied = new HashSet<>();
        occupied.add(new Position(5, 5));

        Optional<Position> result = food.spawn(occupied, grid);
        assertTrue(result.isPresent());
        assertNotEquals(new Position(5, 5), result.get()); // 不在被占据的格子上
        assertTrue(grid.isWithinBounds(result.get()));
    }

    @Test
    @DisplayName("全部格子被占据时 spawn 应返回空")
    void spawnWhenGridFull() {
        Set<Position> allCells = grid.allCells();
        Optional<Position> result = food.spawn(allCells, grid);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("手动放置食物")
    void placeAt() {
        food.placeAt(new Position(10, 10), FoodType.BONUS);
        assertEquals(new Position(10, 10), food.getPosition());
        assertEquals(FoodType.BONUS, food.getType());
        assertEquals(30, food.getScoreValue());
    }

    @Test
    @DisplayName("普通食物分数值应为10")
    void normalFoodScore() {
        food.placeAt(new Position(0, 0), FoodType.NORMAL);
        assertEquals(10, food.getScoreValue());
    }

    @Test
    @DisplayName("奖励食物分数值应为30")
    void bonusFoodScore() {
        food.placeAt(new Position(0, 0), FoodType.BONUS);
        assertEquals(30, food.getScoreValue());
    }
}
