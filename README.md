# 贪吃蛇游戏 (Snake Game)

经典贪吃蛇游戏，使用 Java 17 + Spring Boot 3 + WebSocket + HTML5 Canvas 实现。

## 功能特性

- 🎮 方向键控制蛇的移动，Shift 键加速
- 🌐 支持 Web 浏览器访问
- 💻 支持本机 .exe 启动运行
- 🏆 排行榜（最高分持久化）
- 🎵 背景音乐和音效
- 📈 难度递增

## 快速开始

```bash
# 启动开发服务器
mvn spring-boot:run

# 浏览器访问
http://localhost:8080

# 运行测试
mvn test

# 打包
mvn clean package
```

## 项目结构

- `src/main/java/com/snakegame/engine/` — 纯 Java 游戏引擎
- `src/main/java/com/snakegame/web/` — Spring Boot Web 层
- `src/main/resources/static/` — 前端页面和素材
- `docs/` — 需求文档和开发文档
- `scripts/` — 构建和打包脚本
