# 贪吃蛇游戏 — 第1期开发设计文档

## 1. 架构概述

### 1.1 设计原则
- **引擎零依赖**：核心游戏引擎不依赖任何框架，纯 Java 17 实现
- **单线程模型**：游戏循环由单个 ScheduledExecutorService 线程驱动，避免并发问题
- **观察者模式**：通过 GameEventListener 接口解耦引擎和外部系统
- **不可变性**：关键数据类（Position、GameConfig、GameStateSnapshot）使用 record 实现不可变

### 1.2 技术栈
- Java 17
- JUnit Jupiter 5.10（测试）
- Mockito 5.11（测试）
- JaCoCo 0.8.12（覆盖率）

## 2. 类设计

### 2.1 类图

```
┌─────────────┐     ┌────────────────┐
│  Direction  │────>│   Position     │
│  (enum)     │     │   (record)     │
│  UP/DOWN/   │     │   x: int       │
│  LEFT/RIGHT │     │   y: int       │
│  dx, dy     │     │   move(Dir)    │
│  isOpposite │     └───────┬────────┘
└─────────────┘             │
                      ┌─────┼─────┐
                      │     │     │
                 ┌────▼──┐ ┌▼──────┐ ┌──────────────┐
                 │ Grid  │ │ Snake │ │    Food      │
                 │w/h:int│ │body:LL│ │pos:Position  │
                 │isValid│ │dir:Dir│ │type:FoodType │
                 │freeCel│ │grow() │ │spawn()       │
                 └───┬───┘ │move() │ │placeAt()     │
                     │     │peek() │ └──────┬───────┘
                     │     └───┬───┘        │
                     │         │            │
                 ┌───▼─────────▼────────────▼───┐
                 │         GameEngine           │
                 │  - grid: Grid               │
                 │  - snake: Snake             │
                 │  - food: Food               │
                 │  - config: GameConfig       │
                 │  - scoreTracker: ScoreTracker│
                 │  - state: GameState         │
                 │  + init/start/pause/reset   │
                 │  + setDirection/setSpeedBoost│
                 │  - tick() [核心循环]        │
                 └────────────┬────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │ GameEventListener  │
                    │ (interface)        │
                    │ onStateUpdate()    │
                    │ onEvent()          │
                    └────────────────────┘
```

### 2.2 类详细设计

#### 2.2.1 Direction（枚举）
```java
public enum Direction {
    UP(0, -1), DOWN(0, 1), LEFT(-1, 0), RIGHT(1, 0);
    public final int dx, dy;
    public boolean isOpposite(Direction other);
}
```
- 每个方向包含 x/y 轴的单位偏移量
- `isOpposite()` 通过 dx+dx'=0 且 dy+dy'=0 判断反向

#### 2.2.2 Position（record）
```java
public record Position(int x, int y) {
    public Position move(Direction dir);
}
```
- 不可变坐标类
- `move()` 返回新 Position 而不修改自身

#### 2.2.3 Grid
```java
public class Grid {
    public Grid(int width, int height);
    public boolean isWithinBounds(Position pos);
    public Set<Position> freeCells(Set<Position> occupied);
    public Set<Position> allCells();
}
```
- 管理网格边界
- `freeCells()` 使用双重循环遍历所有格子，排除已占据的

#### 2.2.4 Snake
```java
public class Snake {
    private final LinkedList<Position> body; // head=first
    private Direction direction;
    private int pendingGrowth;

    public Snake(Position startHead, int initialLength, Direction direction);
    public void move();
    public Position peekNextHead();
    public boolean setDirection(Direction newDir);
    public void grow(int amount);
    public boolean occupies(Position pos);
    public boolean isSelfCollidingAt(Position nextHead);
}
```
**关键设计决策：**
- 蛇身使用 `LinkedList<Position>`，头部索引 0
- `peekNextHead()` 与 `move()` 分离：先验证再提交
- `pendingGrowth` 计数器实现延迟增长（每 tick 增长一节直到归零）
- `setDirection()` 返回 false 当方向被拒绝（反向）
- `isSelfCollidingAt()` 考虑增长状态：增长时检查所有身体，非增长时排除尾部（因尾部会移开）

#### 2.2.5 Food
```java
public class Food {
    private Position position;
    private FoodType type;
    private final Random random;

    public Optional<Position> spawn(Set<Position> occupiedCells, Grid grid);
    public void placeAt(Position pos, FoodType type);
}
```
- `spawn()` 从空闲格子随机选择，10% 概率生成奖励食物
- 无空闲格子时返回 `Optional.empty()`

#### 2.2.6 GameEngine
```java
public class GameEngine {
    // 核心组件
    private final Grid grid;
    private final Snake snake;
    private final Food food;
    private final GameConfig config;
    private final ScoreTracker scoreTracker;
    private GameState state;

    // 输入缓冲
    private final AtomicReference<Direction> nextDirection;
    private volatile boolean speedBoost;

    // 游戏循环
    private ScheduledExecutorService gameLoopExecutor;
    private ScheduledFuture<?> tickFuture;

    // 生命周期
    public void init();
    public synchronized void start();
    public synchronized void pause();
    public synchronized void resume();
    public synchronized void reset();
    public void shutdown();

    // 输入
    public void setDirection(Direction dir);
    public void setSpeedBoost(boolean enabled);

    // 核心循环（private）
    private void tick();
    private void gameOver(String reason);
}
```

**游戏循环流程（tick 方法）：**
```
tick() 开始
  │
  ├─ 1. 检查 state == RUNNING（否则返回）
  ├─ 2. 读取输入：从 nextDirection 读取方向变更
  ├─ 3. 计算 nextHead = snake.peekNextHead()
  ├─ 4. 墙壁碰撞检测 → 如果越界 → gameOver()
  ├─ 5. 自身碰撞检测 → 如果碰撞 → gameOver()
  ├─ 6. 食物碰撞检测 → 如果重叠 → grow + addScore + 重新生成食物
  ├─ 7. 提交移动：snake.move()
  ├─ 8. 通知观察者：notifyStateUpdate()
  └─ 9. 调度下次 tick：scheduleTick()
```

**加速机制：**
- `setSpeedBoost(true)` → 取消当前 Future → 以 fastTickMs 重新调度
- `setSpeedBoost(false)` → 取消当前 Future → 以 normalTickMs 重新调度
- 不使用独立的速度状态标志，tick 间隔直接决定速度

## 3. 数据流设计

```
用户输入 ──> setDirection()/setSpeedBoost()
                  │ (volatile/AtomicReference)
                  ▼
            tick() 读取输入
                  │
         ┌───────┼───────┐
         ▼       ▼       ▼
      碰撞检测  食物检测  移动
         │       │       │
         └───────┼───────┘
                 ▼
         GameStateSnapshot
                 │
                 ▼
         GameEventListener.onStateUpdate()
```

## 4. 测试策略

### 4.1 测试层次

| 测试类型 | 覆盖范围 | 工具 |
|----------|----------|------|
| 单元测试 | 每个引擎类的公共方法 | JUnit 5 |
| 模块测试 | GameEngine 完整生命周期 | JUnit 5 + 手动验证 |
| 覆盖率 | 全引擎代码 | JaCoCo |

### 4.2 测试用例设计

**DirectionTest (15 个用例)**
- 4 个方向偏移量验证
- 11 种方向组合的 isOpposite 验证

**PositionTest (8 个用例)**
- 创建和访问、相等性、不可变性、4 方向 move

**GridTest (7 个用例)**
- 创建验证、边界内/外判断、allCells、freeCells

**GameConfigTest (7 个用例)**
- 默认值、自定义值、异常参数验证

**ScoreTrackerTest (7 个用例)**
- 初始值、累加、最高分更新、重置、完全重置

**SnakeTest (14 个用例)**
- 初始长度/位置、移动、peekNextHead 不变性、方向设置/拒绝、增长、占据判断、自碰撞检测

**FoodTest (5 个用例)**
- 生成位置、满格返回空、手动放置、分数值

**GameEngineTest (22 个用例)**
- 初始化、生命周期、移动、方向缓冲、碰撞、食物、加速、分数、快照、事件、异常处理

## 5. 文件清单

```
src/main/java/com/snakegame/engine/
├── Direction.java          # 方向枚举
├── Position.java           # 坐标 record
├── Grid.java               # 网格管理
├── GameConfig.java         # 配置 record
├── GameState.java          # 状态枚举
├── FoodType.java           # 食物类型枚举
├── Snake.java              # 蛇实体
├── Food.java               # 食物管理
├── ScoreTracker.java       # 分数追踪
├── GameEvent.java          # 事件枚举
├── GameEventListener.java  # 观察者接口
├── GameStateSnapshot.java  # 状态快照 record
└── GameEngine.java         # 游戏引擎核心

src/test/java/com/snakegame/engine/
├── DirectionTest.java
├── PositionTest.java
├── GridTest.java
├── GameConfigTest.java
├── ScoreTrackerTest.java
├── SnakeTest.java
├── FoodTest.java
└── GameEngineTest.java
```

---

*文档版本: 1.0 | 日期: 2026-06-19*
