package com.snakegame.engine;

/**
 * 网格中的坐标位置。
 * 使用 Java record 实现不可变性。
 *
 * @param x 列坐标（从左到右递增）
 * @param y 行坐标（从上到下递增）
 */
public record Position(int x, int y) {

    /**
     * 返回沿指定方向移动一步后的新位置。
     *
     * @param dir 移动方向
     * @return 移动后的新 Position
     */
    public Position move(Direction dir) {
        return new Position(x + dir.dx, y + dir.dy);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
