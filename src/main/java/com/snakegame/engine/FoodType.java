package com.snakegame.engine;

/**
 * 食物类型枚举。
 * 不同类型的食物有不同的分数价值和增长量。
 */
public enum FoodType {
    /** 普通食物：基础分数，增长1节 */
    NORMAL(1, 10),

    /** 奖励食物：高分数，增长3节（第2期引入） */
    BONUS(3, 30);

    /** 吃到后蛇身增长节数 */
    public final int growBy;

    /** 获得的分数 */
    public final int scoreValue;

    FoodType(int growBy, int scoreValue) {
        this.growBy = growBy;
        this.scoreValue = scoreValue;
    }
}
