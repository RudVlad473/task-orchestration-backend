package com.vrudenko.kanban_board.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Provide the bounded pool KafkaEventPublisher submits to, so a Kafka publish never runs on the
 * caller's thread whatever the broker's reachability.
 *
 * The pool carries a ContextPropagatingTaskDecorator, which restores the request's observation
 * context on the pool thread, so the publish joins the request's trace instead of starting a new
 * one. The pool rejects a submit when it is full; KafkaEventPublisher counts and logs that.
 */
@Configuration
public class AsyncConfig {
    @Bean(name = "kafkaPublishExecutor")
    public TaskExecutor kafkaPublishExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("kafka-publish-");
        executor.setTaskDecorator(new ContextPropagatingTaskDecorator());
        executor.initialize();
        return executor;
    }
}
