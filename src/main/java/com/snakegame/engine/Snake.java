package com.snakegame.engine;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * 蛇的实体类。
 * 使用 LinkedList 维护蛇身，头部为第一个元素，尾部为最后一个元素。
 */
public class Snake {
    private final LinkedList<Position> body;
    private Direction direction;
    private int pendingGrowth; // 待增长的节数

    /**
     * 创建蛇实例。
     *
     * @param startHead    蛇头起始位置
     * @param initialLength 初始长度
     * @param direction    初始方向
     */
    public Snake(Position startHead, int initialLength, Direction direction) {
        if (initialLength < 2) {
            throw new IllegalArgumentException("蛇初始长度不能小于 2: " + initialLength);
        }
        this.body = new LinkedList<>();
        this.direction = direction;

        // 从头部开始，向反方向延伸生成蛇身
        Position current = startHead;
        for (int i = 0; i < initialLength; i++) {
            body.addLast(current);
            // 下一节在反方向
            current = new Position(current.x() - direction.dx, current.y() - direction.dy);
        }
        this.pendingGrowth = 0;
    }

    /**
     * 在当前位置上移动蛇。
     * 先在头部添加新位置，如果没有待增长则移除尾部。
     * 注意：调用此方法前应使用 peekNextHead() 验证目标位置的安全性。
     */
    public void move() {
        Position newHead = peekNextHead();
        body.addFirst(newHead);

        if (pendingGrowth > 0) {
            pendingGrowth--;
        } else {
            body.removeLast();
        }
    }

    /**
     * 窥探下一步蛇头的位置，不修改蛇身状态。
     * 用于 GameEngine 验证移动的合法性。
     *
     * @return 下一步蛇头将到达的位置
     */
    public Position peekNextHead() {
        return body.getFirst().move(direction);
    }

    /**
     * 设置蛇的移动方向。
     * 如果新方向与当前方向相反，则拒绝更改（防止掉头撞到自己）。
     *
     * @param newDir 新的方向
     * @return 如果方向成功更改返回 true，被拒绝返回 false
     */
    public boolean setDirection(Direction newDir) {
        if (!direction.isOpposite(newDir)) {
            this.direction = newDir;
            return true;
        }
        return false;
    }

    /**
     * 安排蛇在接下来的 N 次移动中每次增长 1 节。
     *
     * @param amount 增长节数
     */
    public void grow(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("增长量不能为负数: " + amount);
        }
        this.pendingGrowth += amount;
    }

    /**
     * 重置蛇到初始状态（用于游戏重新开始）。
     * 清空蛇身，重新以指定位置和方向初始化。
     *
     * @param startHead    新的蛇头位置
     * @param initialLength 新的初始长度
     * @param direction    新的初始方向
     */
    public void reset(Position startHead, int initialLength, Direction direction) {
        this.body.clear();
        this.pendingGrowth = 0;
        this.direction = direction;

        // 从头部向反方向生成身体
        Position current = startHead;
        for (int i = 0; i < initialLength; i++) {
            body.addLast(current);
            current = new Position(current.x() - direction.dx, current.y() - direction.dy);
        }
    }

    /**
     * 检查指定位置是否与蛇身任何部分重叠。
     *
     * @param pos 要检查的位置
     * @return 如果重叠返回 true
     */
    public boolean occupies(Position pos) {
        return body.contains(pos);
    }

    /**
     * 获取蛇身占据的所有格子。
     */
    public Set<Position> occupiedCells() {
        return new HashSet<>(body);
    }

    /**
     * 检查蛇头是否碰到了自己的身体。
     *
     * @param nextHead 下一个蛇头位置
     * @return 如果会发生自碰撞返回 true
     */
    public boolean isSelfCollidingAt(Position nextHead) {
        // 如果蛇没有在增长，尾部会移开，所以最后一个位置不算碰撞
        int checkCount = pendingGrowth > 0 ? body.size() : body.size() - 1;
        int idx = 0;
        for (Position segment : body) {
            if (idx >= checkCount) break;
            if (segment.equals(nextHead)) {
                return true;
            }
            idx++;
        }
        return false;
    }

    /**
     * 检查当前蛇头是否与身体碰撞（用于已移动后的检查）。
     */
    public boolean isSelfColliding() {
        Position head = getHead();
        int idx = 0;
        for (Position segment : body) {
            if (idx == 0) { idx++; continue; } // 跳过头部自己
            if (segment.equals(head)) {
                return true;
            }
            idx++;
        }
        return false;
    }

    // ===== 访问器 =====

    public Position getHead() {
        return body.getFirst();
    }

    public Position getTail() {
        return body.getLast();
    }

    public List<Position> getBody() {
        return List.copyOf(body); // 返回不可变副本
    }

    public Direction getDirection() {
        return direction;
    }

    public int getLength() {
        return body.size();
    }
}
