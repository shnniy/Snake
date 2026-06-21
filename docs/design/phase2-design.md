# 贪吃蛇游戏 — 第2期开发设计文档

## 1. 架构概述

### 1.1 系统架构图

```
┌─────────────────────────────────────────────────┐
│                   浏览器                          │
│  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │ Canvas   │  │ Keyboard │  │  Web Audio    │  │
│  │ Renderer │  │ Handler  │  │  Manager      │  │
│  └────┬─────┘  └────┬─────┘  └───────┬───────┘  │
│       │             │               │           │
│  ┌────┴─────────────┴───────────────┴────────┐  │
│  │        GameClient (game-client.js)        │  │
│  │   - STOMP WebSocket Client               │  │
│  │   - State Management                     │  │
│  │   - UI Controller                        │  │
│  └──────────────────┬───────────────────────┘  │
└─────────────────────┼──────────────────────────┘
                      │ SockJS/STOMP over HTTP
┌─────────────────────┼──────────────────────────┐
│              Spring Boot Server                 │
│  ┌──────────────────┴───────────────────────┐  │
│  │        WebSocketConfig                   │  │
│  │   - /ws endpoint (STOMP)                │  │
│  │   - /topic, /queue broker               │  │
│  │   - /app prefix for client→server       │  │
│  └──────────────────┬───────────────────────┘  │
│                     │                           │
│  ┌──────────────────┴───────────────────────┐  │
│  │         GameController                   │  │
│  │   @MessageMapping("/game/{id}/input")    │  │
│  │   @MessageMapping("/game/{id}/command")  │  │
│  └──────────────────┬───────────────────────┘  │
│                     │                           │
│  ┌──────────────────┴───────────────────────┐  │
│  │       GameSessionManager                 │  │
│  │   - sessions: Map<id, GameEngine>        │  │
│  │   - WebSocket push via SimpMessaging     │  │
│  │   - Listener → DTO conversion            │  │
│  └──────────────────┬───────────────────────┘  │
│                     │                           │
│  ┌──────────────────┴───────────────────────┐  │
│  │         GameEngine (Phase 1)             │  │
│  │   - Core game logic                     │  │
│  │   - Tick loop                            │  │
│  │   - Collision detection                  │  │
│  └──────────────────────────────────────────┘  │
│                                                 │
│  ┌──────────────────────────────────────────┐  │
│  │         AssetGenerator                    │  │
│  │   - PNG generation (BufferedImage)       │  │
│  │   - WAV generation (javax.sound)         │  │
│  └──────────────────────────────────────────┘  │
└─────────────────────────────────────────────────┘
```

### 1.2 数据流

```
1. 页面加载 → GameClient 连接 WebSocket → POST /api/session
2. SessionManager 创建 GameEngine → 注册 Listener
3. 用户按方向键 → InputHandler → GameClient.sendDirection()
4. GameClient → STOMP /app/game/{id}/input → GameController
5. GameController → SessionManager.handleInput()
6. GameEngine.setDirection() [volatile write]
7. 下次 tick: GameEngine 读取方向 → 计算移动 → 通知 Listener
8. Listener → SimpMessagingTemplate.convertAndSend()
9. STOMP → /topic/game/{id} → GameClient.onStateUpdate()
10. GameClient 更新内部状态 → Canvas 渲染（下一帧 requestAnimationFrame）
```

## 2. WebSocket 协议设计

### 2.1 STOMP 端点

| 端点 | 用途 |
|------|------|
| `/ws` | SockJS 握手端点 |
| `/topic/game/{id}` | 服务端推送游戏状态 |
| `/topic/events/{id}` | 服务端推送事件通知 |
| `/app/game/{id}/input` | 客户端发送方向输入 |
| `/app/game/{id}/command` | 客户端发送游戏命令 |

### 2.2 消息格式

**状态消息（Server→Client）：**
```json
{
    "type": "STATE",
    "gameId": "abc12345",
    "snake": [[10, 7], [9, 7], [8, 7]],
    "food": [15, 3],
    "foodType": "NORMAL",
    "score": 30,
    "highScore": 150,
    "gameState": "RUNNING",
    "speedBoost": false,
    "timestamp": 1718800000000
}
```

**输入消息（Client→Server）：**
```json
{
    "type": "INPUT",
    "direction": "UP",
    "speedBoost": true,
    "sequence": 42
}
```

**命令消息（Client→Server）：**
```json
{
    "type": "COMMAND",
    "action": "START"
}
```

**事件消息（Server→Client）：**
```json
{
    "type": "EVENT",
    "gameId": "abc12345",
    "action": "FOOD_EATEN",
    "score": 30,
    "highScore": 150,
    "message": "吃到食物！"
}
```

## 3. 前端设计

### 3.1 组件结构

```
index.html
├── Canvas (#game-canvas)         — 游戏画面渲染
├── Overlay (#overlay)            — 覆盖层（开始/暂停/结束）
│   ├── Title
│   ├── Message
│   ├── Button
│   └── Score
├── HUD (#hud)                    — 信息栏
│   ├── 当前分数
│   ├── 最高分
│   └── 速度指示器
├── Controls (#controls)          — 按钮
│   ├── 开始 (btn-start)
│   ├── 暂停 (btn-pause)
│   └── 重新开始 (btn-restart)
├── Tips (#tips)                  — 操作提示
└── Connection (#connection-status) — 连接状态
```

### 3.2 JavaScript 模块

| 模块 | 文件 | 职责 |
|------|------|------|
| GameClient | game-client.js | WebSocket 连接、状态管理、Canvas 渲染、UI 控制 |
| InputHandler | input-handler.js | 键盘事件监听、方向转换、防抖 |
| AudioManager | audio-manager.js | Web Audio API 音效播放 |

### 3.3 Canvas 渲染细节

- **网格**：半透明线条，颜色 #1a1a35
- **蛇头**：亮绿色 #4CAF50，带白色眼睛和黑色瞳孔，眼睛位置根据方向调整
- **蛇身**：从绿色渐变到深绿色，表示身体分段
- **蛇尾**：深绿色 #2E7D32
- **普通食物**：红色圆形 #E53935，带高光和茎
- **奖励食物**：金色五角星 #FFD700，带光晕
- **加速模式**：蛇头发光效果（shadowBlur），速度指示器变橙色并闪烁

## 4. 素材生成设计

### 4.1 AssetGenerator 结构

```
AssetGenerator.main()
├── generateSnakeHead()    → snake-head.png (32×32, 绿色圆角矩形+眼睛)
├── generateSnakeBody()    → snake-body.png (32×32, 纯绿色圆角矩形)
├── generateSnakeTail()    → snake-tail.png (32×32, 渐变收窄三角形)
├── generateFoodNormal()   → food-normal.png (32×32, 红色圆形+高光+茎)
├── generateFoodBonus()    → food-bonus.png (32×32, 金色五角星)
├── generateBackground()   → background.png (640×480, 深色网格)
├── generateEatSound()     → eat.wav (800Hz, 0.1s 正弦波)
└── generateGameOverSound() → game-over.wav (400→200Hz, 0.5s 下降音)
```

- 使用 `java.awt.image.BufferedImage` + `Graphics2D` 绘制 PNG
- 使用 `javax.sound.sampled` 合成 16-bit PCM WAV 音频
- 构建时由 Maven exec-maven-plugin 在 generate-resources 阶段自动执行

## 5. 文件清单

```
新增文件:
src/main/java/com/snakegame/
├── SnakeGameApplication.java           # Spring Boot 入口
├── web/
│   ├── config/WebSocketConfig.java     # WebSocket STOMP 配置
│   ├── controller/
│   │   ├── GameController.java         # STOMP 消息处理
│   │   └── PageController.java         # HTTP 页面和 REST
│   └── dto/
│       ├── GameStateMessage.java        # 状态消息 DTO
│       ├── InputMessage.java            # 输入消息 DTO
│       ├── CommandMessage.java          # 命令消息 DTO
│       └── EventMessage.java            # 事件消息 DTO
├── service/
│   └── GameSessionManager.java          # 会话管理服务
└── asset/
    └── AssetGenerator.java              # 素材生成器（完整实现）

src/main/resources/static/
├── index.html                           # 游戏主页面
├── css/game.css                         # 样式表
└── js/
    ├── game-client.js                   # 游戏客户端
    ├── input-handler.js                 # 输入处理
    └── audio-manager.js                 # 音频管理
```

---

*文档版本: 1.0 | 日期: 2026-06-19*
