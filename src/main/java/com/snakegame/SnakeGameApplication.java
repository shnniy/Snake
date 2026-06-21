package com.snakegame;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 贪吃蛇游戏 — Spring Boot 应用入口。
 * 启动内嵌 Tomcat 服务器，提供服务端口 8080。
 */
@SpringBootApplication
@EnableScheduling
public class SnakeGameApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnakeGameApplication.class, args);
        System.out.println("========================================");
        System.out.println("  贪吃蛇游戏已启动!");
        System.out.println("  浏览器访问: http://localhost:8080");
        System.out.println("========================================");
    }
}
