package com.example.rabbitmq.demo.consumer;

import com.example.rabbitmq.demo.config.RabbitMQConfig;
import com.example.rabbitmq.demo.model.Order;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class DelayConsumer {

    @RabbitListener(queues = RabbitMQConfig.DELAYED_QUEUE)
    public void receiveMessage(@Payload String msg) {
        System.out.println("[✔] Received message at " + System.currentTimeMillis() + ": " + msg);
    }
}