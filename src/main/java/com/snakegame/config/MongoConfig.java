package com.snakegame.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * 内嵌 MongoDB 配置。
 * 使用 mongo-java-server 提供纯 Java 内存 MongoDB 实例。
 * 手动创建 MongoClient 和 MongoTemplate，确保 MongoServer 先启动。
 * 数据持久化由 HighScoreService 负责（每次写入同步 JSON 文件）。
 */
@Configuration
public class MongoConfig {

    /**
     * 1. 先启动内嵌 MongoDB 服务器（绑定端口）。
     */
    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer server = new MongoServer(new MemoryBackend());
        server.bind("localhost", 27017);
        System.out.println("[MongoDB] 内嵌 MongoDB 已启动: localhost:27017");
        return server;
    }

    /**
     * 2. 等服务器就绪后再创建 MongoClient。
     *      @ConditionalOnMissingBean 确保覆盖 Spring Boot 自动配置的 MongoClient。
     */
    @Bean
    @DependsOn("mongoServer")
    public MongoClient mongoClient() {
        return MongoClients.create("mongodb://localhost:27017");
    }

    /**
     * 3. 基于自定义 MongoClient 创建 MongoTemplate。
     */
    @Bean
    @DependsOn("mongoClient")
    public MongoTemplate mongoTemplate(MongoClient mongoClient) {
        return new MongoTemplate(mongoClient, "snake_game");
    }
}
