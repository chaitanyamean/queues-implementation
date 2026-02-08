package com.url_shortner.project.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE = "sequential-exchange";

    // Main Queue
    public static final String QUEUE_MAIN = "sequential.queue";

    // Retry Queues (Waiting rooms with TTL)
    public static final String QUEUE_RETRY_WAIT = "sequential.retry.wait";
    public static final String QUEUE_SECOND_RETRY_WAIT = "sequential.second.retry.wait";

    // Processing Queues (Where consumers listen)
    // Note: In this simple design, we can use the SAME main/retry listeners,
    // or distinct ones. Let's use distinct ones for clarity and matching the
    // previous architecture.
    public static final String QUEUE_RETRY_PROCESSING = "sequential.retry.processing";
    public static final String QUEUE_SECOND_RETRY_PROCESSING = "sequential.second.retry.processing";

    // DLQ
    public static final String QUEUE_DLQ = "sequential.dlq";

    @Bean
    public org.springframework.amqp.rabbit.core.RabbitAdmin rabbitAdmin(
            org.springframework.amqp.rabbit.connection.ConnectionFactory connectionFactory) {
        return new org.springframework.amqp.rabbit.core.RabbitAdmin(connectionFactory);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    // --- Main Queue ---
    @Bean
    public Queue mainQueue() {
        return QueueBuilder.durable(QUEUE_MAIN).build();
    }

    @Bean
    public Binding mainBinding(Queue mainQueue, TopicExchange exchange) {
        return BindingBuilder.bind(mainQueue).to(exchange).with("sequential.main");
    }

    // --- Retry 1 (1s Delay) ---
    // 1. Wait Queue: Messages sit here for 1s, then DLX to Retry Processing Queue
    @Bean
    public Queue retryWaitQueue() {
        return QueueBuilder.durable(QUEUE_RETRY_WAIT)
                .ttl(1000) // 1s
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey("sequential.retry.process")
                .build();
    }

    @Bean
    public Binding retryWaitBinding(Queue retryWaitQueue, TopicExchange exchange) {
        return BindingBuilder.bind(retryWaitQueue).to(exchange).with("sequential.retry.wait");
    }

    // 2. Processing Queue: Consumer listens here
    @Bean
    public Queue retryProcessingQueue() {
        return QueueBuilder.durable(QUEUE_RETRY_PROCESSING).build();
    }

    @Bean
    public Binding retryProcessingBinding(Queue retryProcessingQueue, TopicExchange exchange) {
        return BindingBuilder.bind(retryProcessingQueue).to(exchange).with("sequential.retry.process");
    }

    // --- Retry 2 (2s Delay) ---
    // 1. Wait Queue: Messages sit here for 2s, then DLX to Second Retry Processing
    // Queue
    @Bean
    public Queue secondRetryWaitQueue() {
        return QueueBuilder.durable(QUEUE_SECOND_RETRY_WAIT)
                .ttl(2000) // 2s
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey("sequential.second.retry.process")
                .build();
    }

    @Bean
    public Binding secondRetryWaitBinding(Queue secondRetryWaitQueue, TopicExchange exchange) {
        return BindingBuilder.bind(secondRetryWaitQueue).to(exchange).with("sequential.second.retry.wait");
    }

    // 2. Processing Queue: Consumer listens here
    @Bean
    public Queue secondRetryProcessingQueue() {
        return QueueBuilder.durable(QUEUE_SECOND_RETRY_PROCESSING).build();
    }

    @Bean
    public Binding secondRetryProcessingBinding(Queue secondRetryProcessingQueue, TopicExchange exchange) {
        return BindingBuilder.bind(secondRetryProcessingQueue).to(exchange).with("sequential.second.retry.process");
    }

    // --- DLQ ---
    @Bean
    public Queue dlq() {
        return QueueBuilder.durable(QUEUE_DLQ).build();
    }

    @Bean
    public Binding dlqBinding(Queue dlq, TopicExchange exchange) {
        return BindingBuilder.bind(dlq).to(exchange).with("sequential.dlq");
    }
}
