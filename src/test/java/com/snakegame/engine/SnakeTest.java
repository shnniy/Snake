package com.snakegame.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Snake 蛇类测试")
class SnakeTest {
    private Snake snake;

    @BeforeEach
    void setUp() {
        // 创建蛇：头部在 (5,5)，向右，初始长度 3
        snake = new Snake(new Position(5, 5), 3, Direction.RIGHT);
    }

    @Test
    @DisplayName("初始蛇身长度应为3")
    void initialLength() {
        assertEquals(3, snake.getLength());
    }

    @Test
    @DisplayName("初始蛇头位置正确")
    void initialHeadPosition() {
        assertEquals(new Position(5, 5), snake.getHead());
    }

    @Test
    @DisplayName("蛇身应从头部向反方向延伸")
    void bodyExtendsOppositeToDirection() {
        List<Position> body = snake.getBody();
        assertEquals(3, body.size());
        // 头部在 (5,5)，身体在左边: (4,5), (3,5)
        assertEquals(new Position(5, 5), body.get(0)); // head
        assertEquals(new Position(4, 5), body.get(1));
        assertEquals(new Position(3, 5), body.get(2)); // tail
    }

    @Test
    @DisplayName("move 应正确前移蛇身")
    void move() {
        snake.move();
        List<Position> body = snake.getBody();
        assertEquals(3, body.size());
        assertEquals(new Position(6, 5), body.get(0)); // new head
        assertEquals(new Position(5, 5), body.get(1));
        assertEquals(new Position(4, 5), body.get(2)); // tail moved up
    }

    @Test
    @DisplayName("peekNextHead 不应修改蛇身")
    void peekNextHeadDoesNotModify() {
        Position next = snake.peekNextHead();
        assertEquals(new Position(6, 5), next);
        assertEquals(new Position(5, 5), snake.getHead()); // head unchanged
        assertEquals(3, snake.getLength());
    }

    @Test
    @DisplayName("setDirection 应成功更改方向")
    void setDirectionSuccess() {
        assertTrue(snake.setDirection(Direction.UP));
        assertEquals(Direction.UP, snake.getDirection());
    }

    @Test
    @DisplayName("setDirection 应拒绝相反方向")
    void setDirectionRejectsOpposite() {
        assertFalse(snake.setDirection(Direction.LEFT)); // 当前向右，不能直接向左
        assertEquals(Direction.RIGHT, snake.getDirection()); // 方向不变
    }

    @Test
    @DisplayName("setDirection 应允许垂直转弯")
    void setDirectionAllowsPerpendicular() {
        assertTrue(snake.setDirection(Direction.UP));
        assertEquals(Direction.UP, snake.getDirection());
        assertTrue(snake.setDirection(Direction.RIGHT));
        assertEquals(Direction.RIGHT, snake.getDirection());
    }

    @Test
    @DisplayName("grow 应延迟增长")
    void growDefersGrowth() {
        snake.grow(2);
        assertEquals(3, snake.getLength()); // 还没移动

        snake.move();
        assertEquals(4, snake.getLength()); // 增长1节

        snake.move();
        assertEquals(5, snake.getLength()); // 增长第2节

        snake.move();
        assertEquals(5, snake.getLength()); // 不再增长
    }

    @Test
    @DisplayName("occupies 应正确判断蛇身占用")
    void occupies() {
        assertTrue(snake.occupies(new Position(5, 5))); // head
        assertTrue(snake.occupies(new Position(4, 5))); // body
        assertTrue(snake.occupies(new Position(3, 5))); // tail
        assertFalse(snake.occupies(new Position(6, 5))); // empty
        assertFalse(snake.occupies(new Position(0, 0)));
    }

    @Test
    @DisplayName("isSelfCollidingAt 应检测到自碰撞")
    void selfCollisionDetected() {
        // 当前蛇 [(5,5), (4,5), (3,5)] 向右
        // 如果下一个位置是 (4,5)（当前身体），应碰撞
        assertTrue(snake.isSelfCollidingAt(new Position(4, 5)));
    }

    @Test
    @DisplayName("isSelfCollidingAt 不应将末尾视为碰撞（因为尾会移开）")
    void tailNotCollisionWhenNotGrowing() {
        // 尾部 (3,5) 在下一步会移开，所以不算碰撞
        assertFalse(snake.isSelfCollidingAt(new Position(3, 5)));
    }

    @Test
    @DisplayName("isSelfCollidingAt 在增长时将末尾视为碰撞")
    void tailCollisionWhenGrowing() {
        snake.grow(1);
        // 增长时尾部不会移开
        assertTrue(snake.isSelfCollidingAt(new Position(3, 5)));
    }

    @Test
    @DisplayName("创建长度小于2的蛇应抛出异常")
    void createTooShortSnake() {
        assertThrows(IllegalArgumentException.class, () ->
            new Snake(new Position(5, 5), 1, Direction.RIGHT));
    }
}
