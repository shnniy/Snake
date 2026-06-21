package com.snakegame.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Position 坐标测试")
class PositionTest {

    @Test
    @DisplayName("创建 Position 并访问坐标")
    void createAndAccess() {
        Position pos = new Position(5, 10);
        assertEquals(5, pos.x());
        assertEquals(10, pos.y());
    }

    @Test
    @DisplayName("相同的坐标应相等")
    void equalsSamePosition() {
        Position p1 = new Position(3, 4);
        Position p2 = new Position(3, 4);
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    @DisplayName("不同的坐标应不相等")
    void notEqualsDifferentPosition() {
        Position p1 = new Position(3, 4);
        Position p2 = new Position(4, 3);
        assertNotEquals(p1, p2);
    }

    @Test
    @DisplayName("move UP 应减少 y 坐标")
    void moveUp() {
        Position pos = new Position(5, 5);
        Position moved = pos.move(Direction.UP);
        assertEquals(new Position(5, 4), moved);
    }

    @Test
    @DisplayName("move DOWN 应增加 y 坐标")
    void moveDown() {
        Position pos = new Position(5, 5);
        Position moved = pos.move(Direction.DOWN);
        assertEquals(new Position(5, 6), moved);
    }

    @Test
    @DisplayName("move LEFT 应减少 x 坐标")
    void moveLeft() {
        Position pos = new Position(5, 5);
        Position moved = pos.move(Direction.LEFT);
        assertEquals(new Position(4, 5), moved);
    }

    @Test
    @DisplayName("move RIGHT 应增加 x 坐标")
    void moveRight() {
        Position pos = new Position(5, 5);
        Position moved = pos.move(Direction.RIGHT);
        assertEquals(new Position(6, 5), moved);
    }

    @Test
    @DisplayName("move 不应修改原坐标（不可变性）")
    void moveDoesNotModifyOriginal() {
        Position original = new Position(5, 5);
        original.move(Direction.UP);
        assertEquals(5, original.x());
        assertEquals(5, original.y());
    }
}
