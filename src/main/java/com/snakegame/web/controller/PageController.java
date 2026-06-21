package com.snakegame.web.controller;

import com.snakegame.persistence.entity.HighScoreEntity;
import com.snakegame.persistence.service.HighScoreService;
import com.snakegame.service.GameSessionManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP 页面控制器。
 * 提供游戏主页面、会话创建、排行榜等 REST 接口。
 */
@Controller
public class PageController {

    private final GameSessionManager sessionManager;
    private final HighScoreService highScoreService;

    public PageController(GameSessionManager sessionManager, HighScoreService highScoreService) {
        this.sessionManager = sessionManager;
        this.highScoreService = highScoreService;
    }

    /**
     * 游戏主页面。
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * 创建新游戏会话并返回 gameId。
     */
    @PostMapping("/api/session")
    @ResponseBody
    public ResponseEntity<Map<String, String>> createSession() {
        String gameId = sessionManager.createSession();
        return ResponseEntity.ok(Map.of("gameId", gameId));
    }

    /**
     * 健康检查端点。
     */
    @GetMapping("/api/health")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "activeSessions", sessionManager.getActiveSessionCount()
        ));
    }

    /**
     * 提交游戏分数。
     */
    @PostMapping("/api/scores")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> submitScore(@RequestBody Map<String, Object> body) {
        String playerName = (String) body.getOrDefault("playerName", "匿名玩家");
        int score = (int) body.getOrDefault("score", 0);
        int snakeLength = (int) body.getOrDefault("snakeLength", 0);
        int foodEaten = (int) body.getOrDefault("foodEaten", 0);

        HighScoreEntity saved = highScoreService.saveScore(playerName, score, snakeLength, foodEaten);

        boolean isTop10 = highScoreService.isTop10(score);
        int rank = -1;
        if (isTop10) {
            List<HighScoreEntity> top10 = highScoreService.getTop10();
            for (int i = 0; i < top10.size(); i++) {
                if (top10.get(i).getId().equals(saved.getId())) {
                    rank = i + 1;
                    break;
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", saved.getId());
        result.put("rank", rank);
        result.put("isTop10", isTop10);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取排行榜前 10 名。
     */
    @GetMapping("/api/scores")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getTop10() {
        List<HighScoreEntity> top10 = highScoreService.getTop10();
        List<Map<String, Object>> result = top10.stream().map(e -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", e.getId());
            map.put("playerName", e.getPlayerName());
            map.put("score", e.getScore());
            map.put("snakeLength", e.getSnakeLength());
            map.put("foodEaten", e.getFoodEaten());
            map.put("createdAt", e.getCreatedAt().toString());
            return map;
        }).toList();
        return ResponseEntity.ok(result);
    }

    /**
     * 获取最高分。
     */
    @GetMapping("/api/scores/highest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHighestScore() {
        int highest = highScoreService.getHighestScore();
        return ResponseEntity.ok(Map.of("highestScore", highest));
    }
}
