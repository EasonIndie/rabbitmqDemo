package com.example.rabbitmq.demo.consumer;

import com.example.rabbitmq.demo.model.Order;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 优先级队列演示消费者
 * 支持暂停/恢复功能，用于验证优先级队列的真实效果
 */
@Slf4j
@Component
public class PriorityDemoConsumer {

    private static final AtomicInteger processedCount = new AtomicInteger(0);
    private static final AtomicInteger pausedCount = new AtomicInteger(0);
    private static volatile boolean consumerPaused = false;

    /**
     * 优先级队列消费者 - 支持暂停控制
     */
    @RabbitListener(queues = "${rabbitmq.queue.order.priority}")
    public void processPriorityOrderWithControl(@Payload Order order,
                                             Channel channel,
                                             @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                             @Header(name = "priority", defaultValue = "5") Integer priority) throws IOException {

        try {
            // 如果消费者被暂停，则等待
            if (consumerPaused) {
                log.info("消费者暂停中，消息等待处理: orderId={}, priority={}",
                        order.getOrderId(), priority);

                // 等待消费者恢复
                while (consumerPaused) {
                    try {
                        Thread.sleep(500); // 每500ms检查一次
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // 记录积压消息数量
                pausedCount.incrementAndGet();
                log.info("消费者已恢复，开始处理积压消息: orderId={}, priority={}, 积压计数: {}",
                        order.getOrderId(), priority, pausedCount.get());
            }

            // 记录处理开始时间
            long startTime = System.currentTimeMillis();
            int totalCount = processedCount.incrementAndGet();

            log.info("开始处理消息 [第{}条] 订单Id: {}, 优先级: {}",
                    totalCount, order.getOrderId(), priority);

            // 更新订单状态
            order.setStatus(Order.OrderStatus.COMPLETED);
            order.setUpdateTime(LocalDateTime.now());

            // 计算处理时间
            long processingTime = System.currentTimeMillis() - startTime;

            log.info("处理完成 [耗时: {}ms] 订单Id: {}, >>>>优先级: {}", processingTime, order.getOrderId(), priority);

            // 手动确认消息
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("消息处理失败: orderId={}, priority={}", order.getOrderId(), priority, e);

            // 拒绝消息并发送到死信队列
            channel.basicNack(deliveryTag, false, false);
        }
    }

    /**
     * 暂停/恢复消费者
     */
    public static void pauseConsumer() {
        consumerPaused = true;
        log.warn("优先级队列消费者已暂停");
    }

    /**
     * 恢复消费者
     */
    public static void resumeConsumer() {
        consumerPaused = false;
        log.info("优先级队列消费者已恢复");
    }

    /**
     * 获取消费者状态
     */
    public static String getConsumerStatus() {
        return String.format("状态: %s, 已处理: %d, 暂停期间积压: %d",
                consumerPaused ? "暂停" : "运行中",
                processedCount.get(),
                pausedCount.get());
    }

    /**
     * 重置计数器
     */
    public static void resetCounters() {
        processedCount.set(0);
        pausedCount.set(0);
        log.info("优先级队列消费者计数器已重置");
    }
}