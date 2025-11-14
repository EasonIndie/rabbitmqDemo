package com.example.rabbitmq.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ配置类
 * 只保留实际使用的配置
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.queue.order.priority}")
    private String orderPriorityQueue;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchange;

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
        //重试配置
        factory.setAdviceChain(RetryInterceptorBuilder.stateless()
                .maxAttempts(3)                   // 重试 3 次
                .backOffOptions(
                        2000,  // 第一次重试等待 2 秒
                        2.0,   // 每次 * 2
                        10000  // 最大等待 10 秒
                )
                .recoverer(new RejectAndDontRequeueRecoverer()) // 重试到上限后: 进入 DLQ
                .build()
        );

        // 设置并发消费者数量
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);

        // 设置预取数量
        factory.setPrefetchCount(1);

        // 开启手动确认
        //factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }


    // 优先级队列

    @Bean
    public Queue orderPriorityQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置最大优先级
        args.put("x-max-priority", 10);
        return QueueBuilder.durable(orderPriorityQueue).withArguments(args).build();
    }

    //交换机配置

    @Bean
    public DirectExchange orderExchange() {
        return ExchangeBuilder.directExchange(orderExchange).durable(true).build();
    }

    // 队列绑定

    // 优先级队列绑定
    @Bean
    public Binding orderPriorityQueueBinding() {
        return BindingBuilder.bind(orderPriorityQueue())
                .to(orderExchange())
                .with("order.priority");
    }


    //=======================延迟配置==========================

    public static final String DELAYED_EXCHANGE = "delayed.exchange";
    public static final String DELAYED_QUEUE = "delayed.queue";
    public static final String ROUTING_KEY = "delayed.key";

    /**
     * 声明一个延迟交换机（x-delayed-message）
     */
    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        // x-delayed-type 表示内部实际使用的路由类型，比如 direct、topic、fanout
        args.put("x-delayed-type", "direct");
        return new CustomExchange(DELAYED_EXCHANGE, "x-delayed-message", true, false, args);
    }

    /**
     * 定义延迟队列
     */
    @Bean
    public Queue delayedQueue() {
        return QueueBuilder.durable(DELAYED_QUEUE).build();
    }

    /**
     * 绑定队列与延迟交换机
     */
    @Bean
    public Binding delayedBinding(Queue delayedQueue, CustomExchange delayedExchange) {
        return BindingBuilder.bind(delayedQueue).to(delayedExchange).with(ROUTING_KEY).noargs();
    }

    //========================重试配置==========================

    public static final String NORMAL_QUEUE = "retry.normal.queue";
    public static final String NORMAL_EXCHANGE = "retry.normal.exchange";

    public static final String DEAD_QUEUE = "retry.dead.queue";
    public static final String DEAD_EXCHANGE = "retry.dead.exchange";

    /**
     * 正常交换机
     */
    @Bean
    public DirectExchange normalExchange() {
        return new DirectExchange(NORMAL_EXCHANGE);
    }

    /**
     * 死信交换机
     */
    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE);
    }

    /**
     * 正常队列，绑定死信交换机
     */
    @Bean
    public Queue normalQueue() {
        Map<String, Object> args = new HashMap<>();
        // 消费失败达到重试次数后，消息进入死信交换机
        args.put("x-dead-letter-exchange", DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", "dead.key");
        return QueueBuilder.durable(NORMAL_QUEUE).withArguments(args).build();
    }

    @Bean
    public Binding normalBinding() {
        return BindingBuilder.bind(normalQueue())
                .to(normalExchange())
                .with("normal.key");
    }

    /**
     * 死信队列
     */
    @Bean
    public Queue deadQueue() {
        return QueueBuilder.durable(DEAD_QUEUE).build();
    }

    @Bean
    public Binding deadBinding() {
        return BindingBuilder.bind(deadQueue())
                .to(deadExchange())
                .with("dead.key");
    }
}