package com.example.rabbitmq.demo.consumer;

import com.example.rabbitmq.demo.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DeadConsumer {

    @RabbitListener(queues = RabbitMQConfig.DEAD_QUEUE)
    public void handleDeadLetter(String msg) {
        System.out.println("❗最终失败进入死信队列：" + msg);
    }
}
