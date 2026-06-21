package com.snakegame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Grid 网格测试")
class GridTest {
    private Grid grid;

    @BeforeEach
    void setUp() {
        grid = new Grid(20, 15);
    }

    @Test
    @DisplayName("创建有效尺寸的网格")
    void createValidGrid() {
        assertEquals(20, grid.getWidth());
        assertEquals(15, grid.getHeight());
    }

    @Test
    @DisplayName("创建小于最小尺寸的网格应抛出异常")
    void createTooSmallGrid() {
        assertThrows(IllegalArgumentException.class, () -> new Grid(5, 5));
        assertThrows(IllegalArgumentException.class, () -> new Grid(10, 9));
        assertThrows(IllegalArgumentException.class, () -> new Grid(9, 10));
    }

    @Test
    @DisplayName("边界内的位置应通过检查")
    void withinBounds() {
        assertTrue(grid.isWithinBounds(new Position(0, 0)));       // 左上角
        assertTrue(grid.isWithinBounds(new Position(19, 14)));     // 右下角
        assertTrue(grid.isWithinBounds(new Position(10, 7)));      // 中心
        assertTrue(grid.isWithinBounds(new Position(0, 14)));      // 左下角
        assertTrue(grid.isWithinBounds(new Position(19, 0)));      // 右上角
    }

    @Test
    @DisplayName("边界外的位置应被拒绝")
    void outOfBounds() {
        assertFalse(grid.isWithinBounds(new Position(-1, 0)));     // 左外
        assertFalse(grid.isWithinBounds(new Position(0, -1)));     // 上外
        assertFalse(grid.isWithinBounds(new Position(20, 0)));     // 右外
        assertFalse(grid.isWithinBounds(new Position(0, 15)));     // 下外
        assertFalse(grid.isWithinBounds(new Position(-1, -1)));    // 对角外
    }

    @Test
    @DisplayName("allCells 应返回所有格子")
    void allCellsCount() {
        Set<Position> all = grid.allCells();
        assertEquals(300, all.size()); // 20 × 15
    }

    @Test
    @DisplayName("freeCells 应排除被占据的格子")
    void freeCellsExcludesOccupied() {
        Set<Position> occupied = new HashSet<>();
        occupied.add(new Position(5, 5));
        occupied.add(new Position(10, 10));

        Set<Position> free = grid.freeCells(occupied);
        assertEquals(298, free.size());
        assertFalse(free.contains(new Position(5, 5)));
        assertFalse(free.contains(new Position(10, 10)));
        assertTrue(free.contains(new Position(0, 0)));
    }

    @Test
    @DisplayName("全部被占据时 freeCells 应返回空集")
    void freeCellsAllOccupied() {
        Set<Position> occupied = grid.allCells();
        Set<Position> free = grid.freeCells(occupied);
        assertTrue(free.isEmpty());
    }
}
