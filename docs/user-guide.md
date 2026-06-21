# 贪吃蛇游戏 — 用户手册

## 1. 系统要求

| 项目 | 最低配置 |
|------|----------|
| 操作系统 | Windows 10/11 |
| Java | JDK 17+ |
| 浏览器 | Chrome/Edge/Firefox 最新版 |
| 内存 | 512 MB 可用 |
| 磁盘 | 50 MB 可用空间 |

## 2. 快速开始

### 2.1 开发模式启动

```bash
# 方式一：Maven 命令
mvn spring-boot:run

# 方式二：开发脚本
scripts/run-dev.bat
```

启动后浏览器访问：**http://localhost:8080**

### 2.2 打包运行

```bash
# 1. 编译打包
mvn clean package -DskipTests

# 2. 运行 Fat JAR
java -jar target/snake-game-1.0.0.jar
```

### 2.3 EXE 运行（第3期）

```bash
# 运行打包脚本
scripts/package-exe.bat

# 启动生成的 EXE
target/installer/SnakeGame/SnakeGame.exe
```

## 3. 游戏操作

### 3.1 基本操作

| 按键 | 功能 |
|------|------|
| **↑ ↓ ← →** | 控制蛇的移动方向 |
| **W A S D** | 替代方向键控制 |
| **Shift** (按住) | 加速移动 |
| **空格键** | 暂停/继续游戏 |
| **开始按钮** | 启动新游戏 |

### 3.2 游戏规则

1. 控制蛇吃到食物，每吃到一个食物分数+10（普通食物）或+30（金色奖励食物）
2. 吃到食物后蛇身长度+1（普通）或+3（奖励）
3. 撞到墙壁 → 游戏结束
4. 撞到自己 → 游戏结束
5. 按住 Shift 可以加速移动，获得更高挑战

### 3.3 界面说明

- **Canvas 画布**：游戏主画面，网格背景+蛇+食物
- **HUD 信息栏**：显示当前分数、最高分、速度状态
- **控制按钮**：开始、暂停、重新开始
- **排行榜**：游戏结束时显示前 10 名最高分
- **连接状态**：显示 WebSocket 连接状态（🟢已连接/🔴断开）

## 4. API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/` | 游戏主页面 |
| POST | `/api/session` | 创建游戏会话 |
| GET | `/api/health` | 健康检查 |
| POST | `/api/scores` | 提交分数 |
| GET | `/api/scores` | 查询排行榜 |
| GET | `/api/scores/highest` | 查询最高分 |
| — | `/ws` | WebSocket 端点 |

## 5. 项目结构

```
Snake/
├── src/main/java/com/snakegame/
│   ├── engine/          # 游戏引擎（纯 Java）
│   ├── web/             # Spring Boot Web 层
│   ├── service/         # 业务服务
│   ├── persistence/     # 数据持久化
│   └── asset/           # 素材生成器
├── src/main/resources/
│   └── static/          # 前端页面 + 素材
├── docs/                # 文档
├── scripts/             # 脚本
└── data/                # 数据库文件（运行时生成）
```

## 6. 常见问题

### Q: 启动后浏览器无法访问？
A: 检查 8080 端口是否被占用。可修改 `application.properties` 中的 `server.port` 配置。

### Q: 游戏画面卡顿？
A: 尝试关闭其他占用带宽的浏览器标签页，或减少同时运行的应用程序。

### Q: 素材文件在哪里？
A: 素材在 Maven 构建时自动生成到 `src/main/resources/static/assets/` 目录。

### Q: 排行榜数据存储在哪里？
A: H2 数据库文件存储在项目根目录的 `data/` 文件夹中。

## 7. 开发者信息

- **技术栈**：Java 17 + Spring Boot 3 + WebSocket + HTML5 Canvas
- **构建工具**：Maven 3.9+
- **测试命令**：`mvn test`
- **打包命令**：`mvn clean package`

---

*文档版本: 1.0 | 日期: 2026-06-19*
