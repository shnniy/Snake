package com.snakegame.engine;

/**
 * 蛇的移动方向枚举。
 * 每个方向包含 x 和 y 轴的单位偏移量。
 */
public enum Direction {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    /** X轴偏移量（每移动一步的列变化） */
    public final int dx;

    /** Y轴偏移量（每移动一步的行变化） */
    public final int dy;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * 判断当前方向与另一个方向是否相反。
     * 用于阻止蛇掉头撞到自己。
     *
     * @param other 另一个方向
     * @return 如果两个方向相反则返回 true
     */
    public boolean isOpposite(Direction other) {
        return this.dx + other.dx == 0 && this.dy + other.dy == 0;
    }
}
