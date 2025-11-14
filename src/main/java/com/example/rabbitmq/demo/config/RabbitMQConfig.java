package com.example.rabbitmq.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 * 演示各种队列和交换机配置
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.order}")
    private String orderQueue;

    @Value("${rabbitmq.queue.order.retry}")
    private String orderRetryQueue;

    @Value("${rabbitmq.queue.order.annotation}")
    private String orderAnnotationQueue;

    @Value("${rabbitmq.queue.order.priority}")
    private String orderPriorityQueue;

    @Value("${rabbitmq.queue.order.delay}")
    private String orderDelayQueue;

    @Value("${rabbitmq.queue.dead-letter}")
    private String deadLetterQueue;

    @Value("${rabbitmq.queue.retry}")
    private String retryQueue;

    @Value("${rabbitmq.queue.retry.failure}")
    private String retryFailureQueue;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

    @Value("${rabbitmq.exchange.delay}")
    private String delayExchange;

    @Value("${rabbitmq.exchange.dead-letter}")
    private String deadLetterExchange;

    @Value("${rabbitmq.exchange.retry}")
    private String retryExchange;

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册Java 8时间模块，支持LocalDateTime等时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期序列化为时间戳，使用ISO-8601格式
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性，避免反序列化时因字段不匹配而失败
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());

        // 开启发送确认
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("消息发送成功: " + correlationData);
            } else {
                System.err.println("消息发送失败: " + correlationData + ", 原因: " + cause);
            }
        });

        // 开启返回确认
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("❌ 消息返回: " + returned.getMessage() +
                             ", 回复码: " + returned.getReplyCode() +
                             ", 回复文本: " + returned.getReplyText() +
                             ", 交换机: " + returned.getExchange() +
                             ", 路由键: " + returned.getRoutingKey());
        });

        return rabbitTemplate;
    }

    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());

        // 设置并发消费者数量
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // 设置预取数量
        factory.setPrefetchCount(1);

        // 开启手动确认
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }

    // ==================== 普通订单队列 ====================

    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(orderQueue).build();
    }

    @Bean
    public Queue orderRetryQueue() {
        return QueueBuilder.durable(orderRetryQueue).build();
    }

    @Bean
    public Queue orderAnnotationQueue() {
        return QueueBuilder.durable(orderAnnotationQueue).build();
    }

    @Bean
    public Queue retryFailureQueue() {
        return QueueBuilder.durable(retryFailureQueue).build();
    }

    // ==================== 优先级队列 ====================

    @Bean
    public Queue orderPriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置最大优先级
        args.put("x-max-priority", 10);
        return QueueBuilder.durable(orderPriorityQueue).withArguments(args).build();
    }

    // ==================== 延迟队列 ====================

    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置消息TTL为15分钟 (900000毫秒)，与已存在的队列保持一致
        args.put("x-message-ttl", 900000);
        // 设置死信交换机
        args.put("x-dead-letter-exchange", orderExchange);
        // 设置死信路由键
        args.put("x-dead-letter-routing-key", "order.process");
        return QueueBuilder.durable(orderDelayQueue).withArguments(args).build();
    }

    // ==================== 死信队列 ====================

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(deadLetterQueue).build();
    }

    // ==================== 重试队列 ====================

    @Bean
    public Queue retryQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置TTL为30秒
        args.put("x-message-ttl", 30000);
        // 设置死信交换机为原始交换机
        args.put("x-dead-letter-exchange", orderExchange);
        // 设置死信路由键为原始队列
        args.put("x-dead-letter-routing-key", "order.retry");
        return QueueBuilder.durable(retryQueue).withArguments(args).build();
    }

    // ==================== 交换机配置 ====================

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(orderExchange).durable(true).build();
    }

    @Bean
    public DirectExchange delayExchange() {
        return ExchangeBuilder.directExchange(delayExchange).durable(true).build();
    }

    @Bean
    public FanoutExchange deadLetterExchange() {
        return ExchangeBuilder.fanoutExchange(deadLetterExchange).durable(true).build();
    }

    @Bean("retryExchangeForBinding")
    public DirectExchange retryExchange() {
        return ExchangeBuilder.directExchange(retryExchange).durable(true).build();
    }

    // ==================== 队列绑定 ====================

    // 普通订单队列绑定
    @Bean
    public Binding orderQueueBinding() {
        return BindingBuilder.bind(orderQueue())
                .to(orderExchange())
                .with("order.process");
    }

    // 重试订单队列绑定
    @Bean
    public Binding orderRetryQueueBinding() {
        return BindingBuilder.bind(orderRetryQueue())
                .to(orderExchange())
                .with("order.retry.process");
    }

    // 注解重试订单队列绑定
    @Bean
    public Binding orderAnnotationQueueBinding() {
        return BindingBuilder.bind(orderAnnotationQueue())
                .to(orderExchange())
                .with("order.annotation.process");
    }

    // 优先级队列绑定
    @Bean
    public Binding orderPriorityQueueBinding() {
        return BindingBuilder.bind(orderPriorityQueue())
                .to(orderExchange())
                .with("order.priority");
    }

    // 重试队列绑定
    @Bean
    public Binding retryQueueBinding() {
        return BindingBuilder.bind(retryQueue())
                .to(orderExchange())
                .with("order.retry");
    }

    // 延迟队列绑定
    @Bean
    public Binding delayQueueBinding() {
        return BindingBuilder.bind(orderDelayQueue())
                .to(delayExchange())
                .with("order.delay");
    }

    // 重试失败队列绑定
    @Bean
    public Binding retryFailureQueueBinding() {
        return BindingBuilder.bind(retryFailureQueue())
                .to(retryExchange())
                .with("retry.failure.key");
    }

    // 死信队列绑定
    @Bean
    public Binding deadLetterQueueBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange());
    }
}