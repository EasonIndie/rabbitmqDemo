package com.example.rabbitmq.demo.controller;

import com.example.rabbitmq.demo.config.RabbitMQConfig;
import com.example.rabbitmq.demo.producer.DelayProducer;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/api/delay")
public class DelayController {


    @Resource
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/send")
    public String sendDelayMessage() {
        long start = System.currentTimeMillis();
        sendDelayMessage("Hello RabbitMQ Delay!", 5000); // 延迟5秒
        return "Message sent at: " + start;
    }


    private void sendDelayMessage(String msg, int delayMillis) {
        // 直接发送字符串消息，让RabbitTemplate自动处理序列化
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DELAYED_EXCHANGE,
                RabbitMQConfig.ROUTING_KEY,
                msg,
                message -> {
                    message.getMessageProperties().setHeader("x-delay", delayMillis);
                    return message;
                }
        );

        System.out.println("[x] Sent delayed message: " + msg + " with delay " + delayMillis + "ms");
    }
}