package com.example.rabbitmq.demo.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.listener.RetryListenerSupport;
import org.springframework.stereotype.Component;

/**
 * 注解重试监听器
 * 用于监听@Retryable注解的重试过程
 */
@Slf4j
@Component("annotationRetryListener")
public class AnnotationRetryListener extends RetryListenerSupport {

    @Override
    public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        log.info("🔄 [注解重试] 开始重试监听");
        return super.open(context, callback);
    }

    @Override
    public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        if (throwable == null) {
            log.info("✅ [注解重试] 重试成功，总重试次数: {}", context.getRetryCount());
        } else {
            log.error("❌ [注解重试] 重试失败，总重试次数: {}", context.getRetryCount());
        }
        super.close(context, callback, throwable);
    }

    @Override
    public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        int retryCount = context.getRetryCount();
        log.warn("🔄 [注解重试] 第{}次重试失败: {}", retryCount, throwable.getMessage());
        super.onError(context, callback, throwable);
    }
}