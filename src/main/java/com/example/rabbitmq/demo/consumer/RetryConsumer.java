package com.example.rabbitmq.demo.consumer;

import com.example.rabbitmq.demo.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RetryConsumer {

    private int count = 0;

    @RabbitListener(
            queues = RabbitMQConfig.NORMAL_QUEUE
    )
    public void handleMessage(String msg) {
        count++;
        System.out.println("处理消息，第 " + count + " 次：" + msg);

        // 故意抛异常，让其重试
        throw new RuntimeException("模拟消费失败");
    }
}
