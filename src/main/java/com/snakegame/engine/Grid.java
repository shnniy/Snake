package com.snakegame.engine;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 游戏网格。
 * 管理网格边界和空闲格子的计算。
 */
public class Grid {
    private final int width;
    private final int height;

    /**
     * 创建指定大小的网格。
     *
     * @param width  网格宽度（列数）
     * @param height 网格高度（行数）
     * @throws IllegalArgumentException 如果尺寸小于最小值
     */
    public Grid(int width, int height) {
        if (width < 10 || height < 10) {
            throw new IllegalArgumentException("网格尺寸不能小于 10×10，当前: " + width + "×" + height);
        }
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * 判断位置是否在网格边界内。
     *
     * @param pos 要检查的位置
     * @return 如果在边界内返回 true
     */
    public boolean isWithinBounds(Position pos) {
        return pos.x() >= 0 && pos.x() < width
            && pos.y() >= 0 && pos.y() < height;
    }

    /**
     * 获取所有空闲格子（未被占据的格子）。
     *
     * @param occupied 已被占据的格子集合
     * @return 空闲格子集合
     */
    public Set<Position> freeCells(Set<Position> occupied) {
        Set<Position> free = new HashSet<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Position p = new Position(x, y);
                if (!occupied.contains(p)) {
                    free.add(p);
                }
            }
        }
        return free;
    }

    /**
     * 获取网格中所有的格子位置。
     */
    public Set<Position> allCells() {
        Set<Position> cells = new HashSet<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                cells.add(new Position(x, y));
            }
        }
        return cells;
    }

    @Override
    public String toString() {
        return "Grid{" + width + "×" + height + "}";
    }
}
