# 贪吃蛇游戏 — 第3期开发设计文档

## 1. 持久化层设计

### 1.1 数据库选型
- **H2 文件数据库**：零安装、内嵌运行、文件持久化
- 数据库文件路径：`./data/snake-highscores.mv.db`
- 连接配置：`jdbc:h2:file:./data/snake-highscores;DB_CLOSE_DELAY=-1`

### 1.2 实体设计

```java
@Entity
@Table(name = "high_scores")
public class HighScoreEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String playerName;    // 玩家名称
    private int score;            // 分数
    private int snakeLength;      // 蛇最终长度
    private int foodEaten;        // 吃掉的食物数
    private LocalDateTime createdAt; // 创建时间
}
```

### 1.3 仓库接口

```java
@Repository
public interface HighScoreRepository extends JpaRepository<HighScoreEntity, Long> {
    List<HighScoreEntity> findTop10ByOrderByScoreDesc();
    HighScoreEntity findTopByOrderByScoreDesc();
}
```

### 1.4 服务层

```java
@Service
@Transactional
public class HighScoreService {
    HighScoreEntity saveScore(...);
    List<HighScoreEntity> getTop10();
    int getHighestScore();
    boolean isTop10(int score);
}
```

### 1.5 分数自动保存流程

```
GameEngine → tick() → gameOver()
  → notifyEvent(GAME_OVER)
    → GameSessionManager.onEvent()
      → highScoreService.saveScore(...)
      → 数据持久化到 H2 数据库
```

## 2. 排行榜 API 设计

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/scores` | 提交分数 |
| GET | `/api/scores` | 查询前 10 名 |
| GET | `/api/scores/highest` | 查询最高分 |

## 3. EXE 打包设计

### 3.1 工具选型
- **jpackage**（JDK 17 内置）：生成原生 Windows 可执行文件
- 无需 WiX Toolset 时可退化为 `app-image` 模式

### 3.2 打包流程

```
1. mvn clean package -DskipTests  → 生成 Fat JAR
2. jpackage --type exe            → 生成 EXE 安装包
   ├── 主方案：--type exe（需 WiX）
   └── 备用方案：--type app-image（无需 WiX）
```

### 3.3 启动行为
1. 用户双击 SnakeGame.exe
2. Spring Boot 启动内嵌 Tomcat（端口 8080）
3. 控制台输出 "贪吃蛇游戏已启动! 浏览器访问: http://localhost:8080"
4. 用户打开浏览器访问该地址

## 4. 前端排行榜集成

### 4.1 数据流

```
游戏结束 → GameClient.showOverlay('gameover')
  → fetchLeaderboard()
    → GET /api/scores
    → 渲染 <table> 排行榜
    → 显示在覆盖层中
```

### 4.2 UI 组件

```
Overlay
├── 游戏结束标题
├── 最终得分
├── 排行榜表格 (#leaderboard-table)
│   ├── 排名 #
│   ├── 玩家名称
│   ├── 分数
│   └── 蛇长度
└── 重新开始按钮
```

## 5. 文件清单（第3期新增）

```
src/main/java/com/snakegame/persistence/
├── entity/HighScoreEntity.java
├── repository/HighScoreRepository.java
└── service/HighScoreService.java

src/main/resources/application.properties  (更新：H2 配置)
src/main/web/controller/PageController.java (更新：排行榜 API)
src/main/service/GameSessionManager.java   (更新：自动保存分数)
src/main/resources/static/
├── index.html           (更新：排行榜 HTML)
├── css/game.css         (更新：排行榜样式)
└── js/game-client.js    (更新：fetchLeaderboard)

scripts/
├── run-dev.bat          (开发启动脚本)
└── package-exe.bat      (EXE 打包脚本)

docs/
├── requirements/phase3-requirements.md
├── design/phase3-design.md
├── test-reports/phase3-test-report.md
└── user-guide.md
```

---

*文档版本: 1.0 | 日期: 2026-06-19*
