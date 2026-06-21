package com.snakegame.persistence.service;

import com.snakegame.persistence.entity.HighScoreEntity;
import com.snakegame.persistence.repository.HighScoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 排行榜服务。
 * 管理最高分记录的保存和查询。
 */
@Service
@Transactional
public class HighScoreService {

    private final HighScoreRepository repository;

    public HighScoreService(HighScoreRepository repository) {
        this.repository = repository;
    }

    /**
     * 保存新的高分记录。
     */
    public HighScoreEntity saveScore(String playerName, int score, int snakeLength, int foodEaten) {
        HighScoreEntity entity = new HighScoreEntity(playerName, score, snakeLength, foodEaten);
        return repository.save(entity);
    }

    /**
     * 获取排行榜前 10 名。
     */
    @Transactional(readOnly = true)
    public List<HighScoreEntity> getTop10() {
        return repository.findTop10ByOrderByScoreDesc();
    }

    /**
     * 获取历史最高分。
     */
    @Transactional(readOnly = true)
    public int getHighestScore() {
        HighScoreEntity top = repository.findTopByOrderByScoreDesc();
        return top != null ? top.getScore() : 0;
    }

    /**
     * 判断分数是否进入前 10 名。
     */
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public long getTotalCount() {
        return repository.count();
    }
}
