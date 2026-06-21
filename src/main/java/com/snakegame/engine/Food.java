package com.snakegame.engine;

import java.util.*;

/**
 * 食物管理类。
 * 负责食物的随机生成和管理。
 */
public class Food {
    private Position position;
    private FoodType type;
    private final Random random;

    /**
     * 创建食物管理器。
     */
    public Food() {
        this.random = new Random();
        this.position = new Position(0, 0);
        this.type = FoodType.NORMAL;
    }

    /**
     * 在空闲格子中随机放置食物。
     *
     * @param occupiedCells 被蛇占据的格子集合
     * @param grid          游戏网格
     * @return 新食物位置的 Optional，如果没有空闲格子则返回 Optional.empty()
     */
    public Optional<Position> spawn(Set<Position> occupiedCells, Grid grid) {
        Set<Position> free = grid.freeCells(occupiedCells);

        if (free.isEmpty()) {
            return Optional.empty(); // 蛇占满整个网格，游戏胜利
        }

        // 从空闲格子中随机选择
        List<Position> freeList = new ArrayList<>(free);
        int index = random.nextInt(freeList.size());
        this.position = freeList.get(index);

        // 10% 概率生成奖励食物
        this.type = random.nextDouble() < 0.1 ? FoodType.BONUS : FoodType.NORMAL;

        return Optional.of(this.position);
    }

    /**
     * 在指定位置放置食物（用于测试）。
     */
    public void placeAt(Position pos, FoodType type) {
        this.position = pos;
        this.type = type;
    }

    public Position getPosition() {
        return position;
    }

    public FoodType getType() {
        return type;
    }

    public int getScoreValue() {
        return type.scoreValue;
    }

    @Override
    public String toString() {
        return "Food{pos=" + position + ", type=" + type + "}";
    }
}
