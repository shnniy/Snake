package com.snakegame.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Direction 枚举测试")
class DirectionTest {

    @Test
    @DisplayName("UP 的偏移量应为 (0, -1)")
    void upDxDy() {
        assertEquals(0, Direction.UP.dx);
        assertEquals(-1, Direction.UP.dy);
    }

    @Test
    @DisplayName("DOWN 的偏移量应为 (0, 1)")
    void downDxDy() {
        assertEquals(0, Direction.DOWN.dx);
        assertEquals(1, Direction.DOWN.dy);
    }

    @Test
    @DisplayName("LEFT 的偏移量应为 (-1, 0)")
    void leftDxDy() {
        assertEquals(-1, Direction.LEFT.dx);
        assertEquals(0, Direction.LEFT.dy);
    }

    @Test
    @DisplayName("RIGHT 的偏移量应为 (1, 0)")
    void rightDxDy() {
        assertEquals(1, Direction.RIGHT.dx);
        assertEquals(0, Direction.RIGHT.dy);
    }

    @ParameterizedTest
    @CsvSource({
        "UP, DOWN, true",
        "DOWN, UP, true",
        "LEFT, RIGHT, true",
        "RIGHT, LEFT, true",
        "UP, UP, false",
        "UP, LEFT, false",
        "UP, RIGHT, false",
        "DOWN, LEFT, false",
        "DOWN, RIGHT, false",
        "LEFT, UP, false",
        "RIGHT, UP, false"
    })
    @DisplayName("isOpposite 应正确判断相反方向")
    void isOpposite(Direction d1, Direction d2, boolean expected) {
        assertEquals(expected, d1.isOpposite(d2));
    }
}
