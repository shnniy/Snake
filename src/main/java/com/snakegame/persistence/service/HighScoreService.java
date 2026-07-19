package com.snakegame.persistence.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.snakegame.persistence.entity.HighScoreEntity;
import com.snakegame.persistence.repository.HighScoreRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 排行榜服务。
 * MongoDB 作为运行时存储 + JSON 文件作为持久化备份。
 * 每次写入同时刷新到 JSON 文件，启动时自动恢复。
 */
@Service
public class HighScoreService {

    private final HighScoreRepository repository;
    private final ObjectMapper objectMapper;

    @Value("${game.mongodb.data.dir:./data/mongodb}")
    private String dataDir;

    public HighScoreService(HighScoreRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 启动时从 JSON 文件恢复数据到 MongoDB（内存）。
     * 注意：@PostConstruct 在此 Bean 初始化后执行，
     * Spring Data MongoDB 自动配置确保 MongoTemplate 已就绪。
     */
    @PostConstruct
    public void restoreFromFile() {
        Path filePath = getDataFilePath();
        if (!Files.exists(filePath)) {
            System.out.println("[持久化] 数据文件不存在，跳过恢复: " + filePath.toAbsolutePath());
            return;
        }
        try {
            String json = Files.readString(filePath);
            if (json.isBlank()) {
                return;
            }
            List<HighScoreEntity> scores = objectMapper.readValue(json,
                new TypeReference<List<HighScoreEntity>>() {});
            if (!scores.isEmpty()) {
                // 清空 MongoDB 中的旧数据，再批量写入
                repository.deleteAll();
                repository.saveAll(scores);
                System.out.println("[持久化] 已从文件恢复 " + scores.size() + " 条记录: " + filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[持久化] 恢复数据失败: " + e.getMessage());
        }
    }

    /**
     * 保存新的高分记录，同时立即持久化到 JSON 文件。
     */
    public HighScoreEntity saveScore(String playerName, int score, int snakeLength, int foodEaten) {
        HighScoreEntity entity = new HighScoreEntity(playerName, score, snakeLength, foodEaten);
        HighScoreEntity saved = repository.save(entity);
        flushToFile();
        return saved;
    }

    /**
     * 获取排行榜前 10 名。
     */
    public List<HighScoreEntity> getTop10() {
        return repository.findTop10ByOrderByScoreDesc();
    }

    /**
     * 获取历史最高分。
     */
    public int getHighestScore() {
        HighScoreEntity top = repository.findTopByOrderByScoreDesc();
        return top != null ? top.getScore() : 0;
    }

    /**
     * 判断分数是否进入前 10 名。
     */
    public boolean isTop10(int score) {
        List<HighScoreEntity> top10 = repository.findTop10ByOrderByScoreDesc();
        if (top10.size() < 10) {
            return true;
        }
        return score > top10.get(top10.size() - 1).getScore();
    }

    /**
     * 获取排行榜总数。
     */
    public long getTotalCount() {
        return repository.count();
    }

    /**
     * 立即将 MongoDB 中的所有数据写入 JSON 文件。
     */
    private void flushToFile() {
        Path filePath = getDataFilePath();
        try {
            Files.createDirectories(filePath.getParent());
            List<HighScoreEntity> all = repository.findAll();
            // 按分数降序排列，只保留前 100 条
            all.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
            if (all.size() > 100) {
                all = all.subList(0, 100);
                // 删除多余的旧记录
                repository.deleteAll();
                repository.saveAll(all);
            }
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(all);
            Files.writeString(filePath, json);
            System.out.println("[持久化] 已保存 " + all.size() + " 条记录: " + filePath.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("[持久化] 保存失败: " + e.getMessage());
        }
    }

    private Path getDataFilePath() {
        return Paths.get(dataDir, "scores.json");
    }
}
