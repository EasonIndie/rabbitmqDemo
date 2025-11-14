package com.example.rabbitmq.demo.controller;

import com.example.rabbitmq.demo.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 企业级方案：Spring Retry + DLQ
 * ——简洁、安全、不阻塞。
 * Consumer 处理失败
 *        ↓
 * Spring Retry （例如重试 3 次）
 *        ↓
 * 重试仍失败 → 发送到 DLX（死信交换机）
 *        ↓
 * 进入死信队列 (dead.queue)
 *        ↓
 * 可以记录日志、人工处理、发钉钉通知等
 */

@RestController
@RequestMapping("/api/retry")
public class RetryDemoController {

    @GetMapping("/send")
    public String test() {
        send("Hello Retry MQ");
        return "success";
    }

    @Resource
    private RabbitTemplate rabbitTemplate;

    private void send(String msg) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.NORMAL_EXCHANGE,
                "normal.key",
                msg
        );
        System.out.println("发送消息：" + msg);
    }
}
