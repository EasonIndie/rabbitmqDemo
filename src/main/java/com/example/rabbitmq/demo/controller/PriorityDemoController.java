package com.example.rabbitmq.demo.controller;

import com.example.rabbitmq.demo.consumer.PriorityDemoConsumer;
import com.example.rabbitmq.demo.model.Order;
import com.example.rabbitmq.demo.model.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 简化的优先级队列演示控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/priority")
@RequiredArgsConstructor
public class PriorityDemoController {

    private final RabbitTemplate rabbitTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 批量发送测试 - 验证优先级队列的真实效果
     */
    @PostMapping("/batch-test")
    public String batchTest(
            @RequestParam(defaultValue = "20") int lowPriorityCount,
            @RequestParam(defaultValue = "10") int highPriorityCount) {

        log.info("开始批量优先级测试: 低优先级{}条, 高优先级{}条", lowPriorityCount, highPriorityCount);

        PriorityDemoConsumer.resetCounters();

        CompletableFuture.runAsync(() -> {
            try {
                // 第一阶段：发送大量低优先级消息
                log.info("第一阶段：发送{}条低优先级消息", lowPriorityCount);

                for (int i = 1; i <= lowPriorityCount; i++) {
                    final int index = i;
                    Order order = createTestOrder("LOW-" + index, "BATCH_USER");
                    order.setPriority(1);

                    rabbitTemplate.convertAndSend(
                        "order.tech_share.exchange",
                        "order.priority",
                        order,
                        message -> {
                            message.getMessageProperties().setPriority(1);
                            return message;
                        }
                    );

                    if (i % 100 == 0) {
                        log.info("已发送低优先级消息: {}/{}", i, lowPriorityCount);
                    }
                }

                Thread.sleep(1000);

                // 第二阶段：发送高优先级消息
                log.info("第二阶段：发送{}条高优先级消息", highPriorityCount);

                for (int i = 1; i <= highPriorityCount; i++) {
                    final int index = i;
                    Order order = createTestOrder("HIGH-" + index, "VIP_USER");
                    order.setPriority(10);

                    rabbitTemplate.convertAndSend(
                        "order.tech_share.exchange",
                        "order.priority",
                        order,
                        message -> {
                            message.getMessageProperties().setPriority(10);
                            return message;
                        }
                    );
                }

                log.info("批量优先级测试发送完成！");
                log.info("预期效果：高优先级消息应该插队到队列前面优先处理");

            } catch (Exception e) {
                log.error("批量优先级测试失败", e);
            }
        }, executorService);

        return String.format(
            "批量优先级测试已启动！\n" +
            "低优先级消息: %d条 (优先级=1)\n" +
            "高优先级消息: %d条 (优先级=10)\n" +
            "预期效果：高优先级消息应该优先被处理",
            lowPriorityCount, highPriorityCount);
    }

    /**
     * 暂停消费者
     */
    @PostMapping("/pause")
    public String pauseConsumer() {
        PriorityDemoConsumer.pauseConsumer();
        return "消费者已暂停，消息开始积压...";
    }

    /**
     * 恢复消费者
     */
    @PostMapping("/resume")
    public String resumeConsumer() {
        PriorityDemoConsumer.resumeConsumer();
        return "消费者已恢复，开始处理积压消息...";
    }

    /**
     * 消费者状态查询
     */
    @GetMapping("/status")
    public String getStatus() {
        return PriorityDemoConsumer.getConsumerStatus();
    }

    /**
     * 重置计数器
     */
    @PostMapping("/reset")
    public String resetCounters() {
        PriorityDemoConsumer.resetCounters();
        return "计数器已重置";
    }

    private Order createTestOrder(String orderIdPrefix, String userId) {
        Order order = new Order();
        order.setOrderId(orderIdPrefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8));
        order.setUserId(userId);

        List<OrderItem> items = new ArrayList<>();
        items.add(new OrderItem(
            "PROD001",
            "Test Product",
            new BigDecimal("100.00"),
            1
        ));

        order.setItems(items);
        order.setCreateTime(LocalDateTime.now());
        order.setStatus(Order.OrderStatus.PENDING);
        order.calculateTotalAmount();

        return order;
    }
}