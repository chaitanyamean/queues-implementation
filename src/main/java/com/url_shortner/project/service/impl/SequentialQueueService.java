package com.url_shortner.project.service.impl;

import com.url_shortner.project.config.RabbitMQConfig;
import com.url_shortner.project.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SequentialQueueService {

    // DLQ
    public static final String QUEUE_DLQ = "sequential.dlq";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private EmailService emailService;

    private final java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors
            .newScheduledThreadPool(10); // increased pool for scheduling

    // --- Publisher (Immediate) ---

    public void enqueue(String item) {
        System.out.println("[SequentialService] Enqueuing to RabbitMQ: " + item);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "sequential.main", item);
    }

    // --- Publisher (Scheduled) ---
    // [Q11] "Calculate difference and add that much delay"
    public void scheduleEmail(com.url_shortner.project.dto.EmailRequest request) {
        long delay = 0;
        if (request.getScheduledTime() != null) {
            java.time.Duration duration = java.time.Duration.between(java.time.LocalDateTime.now(),
                    request.getScheduledTime());
            delay = duration.toMillis();
        }

        String taskMessage = "Email to " + request.getEmail() + ": " + request.getSubject();

        if (delay <= 0) {
            System.out.println(
                    "[SequentialService] ⚡ Scheduled time is in the past/now. Enqueuing immediately: " + taskMessage);
            enqueue(taskMessage);
        } else {
            System.out.println("[SequentialService] 🕒 Scheduling email for " + request.getScheduledTime() + " (in "
                    + delay + "ms): " + taskMessage);
            // "Add that much delay while adding the task to the queue"
            scheduler.schedule(() -> {
                System.out.println("[SequentialService] ⏰ Delay expired. Enqueuing now: " + taskMessage);
                enqueue(taskMessage);
            }, delay, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    // --- Consumers ---

    @RabbitListener(queues = "sequential.queue")
    public void processQueue(String task) {
        try {
            processTask(task, "Sequential-Rabbit-Worker");
        } catch (Exception e) {
            System.err.println("   [Sequential-Rabbit-Worker] ❌ Error processing task " + task + ": " + e.getMessage());
            System.err.println("   [Sequential-Rabbit-Worker] 🕒 Scheduling retry in 1s -> Retry Wait Queue");

            // Publish to Retry Wait Queue (TTL 1s -> Retry Process)
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "sequential.retry.wait", task);
        }
    }

    @RabbitListener(queues = "sequential.retry.processing")
    public void processRetryQueue(String task) {
        try {
            System.out.println("   [Retry-Rabbit-Worker] ♻️ Retrying task: " + task);
            processTask(task, "Retry-Rabbit-Worker");
        } catch (Exception e) {
            System.err.println(
                    "   [Retry-Rabbit-Worker] ❌ Failed again. Scheduling retry in 2s -> Second Retry Wait Queue");

            // Publish to Second Retry Wait Queue (TTL 2s -> Second Retry Process)
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "sequential.second.retry.wait", task);
        }
    }

    @RabbitListener(queues = "sequential.second.retry.processing")
    public void processSecondRetryQueue(String task) {
        try {
            System.out.println("   [Second-Retry-Rabbit-Worker] ♻️ Retrying task (2nd attempt): " + task);
            processTask(task, "Second-Retry-Rabbit-Worker");
        } catch (Exception e) {
            System.err.println(
                    "   [Second-Retry-Rabbit-Worker] ❌ Permanently Failed task " + task + ": " + e.getMessage());
            System.err.println("   [Second-Retry-Rabbit-Worker] 💀 Moving to Dead Letter Queue (DLQ): " + task);

            // Limit Reached -> DLQ
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "sequential.dlq", task);
        }
    }

    @RabbitListener(queues = "sequential.dlq")
    public void processDeadLetterQueue(String task) {
        System.out.println("   [DLQ-Rabbit-Worker] 📩 Received dead letter: " + task);
        // Alerting [Q9]
        emailService.sendEmail("admin@example.com", "DLQ Alert: Task Failed",
                "The following task failed all retries: " + task);
    }

    private void processTask(String task, String workerName) throws InterruptedException {
        System.out.println("   [" + workerName + "] ▶ Starting task: " + task);

        // Simulate processing time
        Thread.sleep(1000);

        System.out.println("   [" + workerName + "] ✅ Finished task: " + task);
    }
}
