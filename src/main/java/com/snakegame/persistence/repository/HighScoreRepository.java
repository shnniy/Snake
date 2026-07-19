package com.snakegame.persistence.repository;

import com.snakegame.persistence.entity.HighScoreEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 最高分数据仓库。
 * 使用 Spring Data MongoDB 自动生成 CRUD 操作。
 */
@Repository
public interface HighScoreRepository extends MongoRepository<HighScoreEntity, String> {

    /**
     * 查询前 N 名最高分，按分数降序排列。
     */
    List<HighScoreEntity> findTop10ByOrderByScoreDesc();

    /**
     * 查询数据库中的最高分。
     */
    HighScoreEntity findTopByOrderByScoreDesc();
}
