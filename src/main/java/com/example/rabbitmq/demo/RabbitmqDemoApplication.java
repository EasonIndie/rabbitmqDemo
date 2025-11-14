package com.example.rabbitmq.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * RabbitMQ Demo Application
 *
 * 演示RabbitMQ各种高级特性：
 * 1. 消息确认机制
 * 2. 死信队列
 * 3. 延迟队列
 * 4. 优先级队列
 * 5. 消息重试
 * 6. 事务消息
 * 7. 消息路由
 * 8. 集群配置
 * 9. 监控指标
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableRetry
public class RabbitmqDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitmqDemoApplication.class, args);
        System.out.println("==========================================");
        System.out.println("🚀 RabbitMQ Demo Application Started!");
        System.out.println("==========================================");
    }
}