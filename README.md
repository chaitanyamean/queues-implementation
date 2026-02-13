# Sequential Queue Error Handling

## [Q1] Implement try/catch in the worker. If an error occurs while processing, enqueue the message back to the queue.

We implemented a "fast retry" mechanism by catching errors in the worker and immediately pushing the failed task back to the end of the queue.

https://www.loom.com/share/5deb2c77130e473f96d377689eaeee2f

**Pros**:
-   **Data Safety**: Messages aren't lost.

**Cons**:
-   **Infinite Loops**: "Poison messages" (always failing) clog the queue.
-   **Order Disruption**: Failed items lose their original position.
-   **Head-of-Line Blocking**: Only relative to the worker, but a frequently failing item can consume mostly all resources.

---

## [Q2] Retry Queue Strategy: When an error occurs, add the message to a new retry queue. Add a new worker to process this new queue.

We introduced a **Retry Queue** (`retryQueue`) and a dedicated **Retry Worker**.

### Implementation Details
1.  **Dual Queues**:
    -   `queue`: Main processing queue (Fast).
    -   `retryQueue`: Holds failed tasks (potentially Slow).
2.  **Dual Workers**:
    -   `Sequential-Worker`: Consumes `queue`. If a task fails, it moves it to `retryQueue`.
    -   `Retry-Worker`: Consumes `retryQueue`. It processes tasks independently.

### Code Logic
```java
// Sequential-Worker
try {
    processTask(task);
} catch (Exception e) {
    System.err.println("❌ Error. Moving to Retry Queue: " + task);
    retryQueue.offer(task); // Offload the problem
}

// Retry-Worker
try {
    processTask(retryTask);
} catch (Exception e) {
    System.err.println("❌ Permanently Failed: " + retryTask);
    // Send to Dead Letter Queue (DLQ)
}
```

---

## [Q3] What are the benefits of this approach?

### 1. Fault Isolation (The "Quarantine" Benefits)
By moving the failed task to a separate queue, we "quarantine" the problem. The main worker is immediately free to process the next healthy task (e.g., Task 'D').
-   **Main Queue**: `[A, B, C(fail), D]` -> `Sequential-Worker` moves `C` to Retry Queue -> continues with `D` immediately.

### 2. Improved Throughput
A single failing task (or a slow failing resource) does not block the entire system. Healthy tasks continue to flow through the main queue at full speed.

### 3. Dedicated Retry Policy
The `Retry-Worker` can have different characteristics:
-   **Throttling**: It can sleep longer between tasks to avoid overwhelming the downstream service.
-   **Alerting**: It can trigger specialized alerts for the operations team.

### 4. Prevention of Poison Pill Loops
The main worker doesn't see the same failed message again. We avoid the "Infinite Loop" scenario in the primary processing path.

---

## [Q4] Add another retry queue (Second Retry Queue) and a corresponding worker.

We implemented a chain of queues:
`Main Queue` -> `Retry Queue` -> `Second Retry Queue` -> `DLQ (Log/Drop)`

If `Retry-Worker` fails to process a task, it moves it to `secondRetryQueue`. A `Second-Retry-Worker` picks it up for one final attempt.

**Logs Observation:**
```
[Sequential-Worker] ❌ Error processing task C
[Sequential-Worker] 🔄 Moving task to Retry Queue: C
[Retry-Worker] ⚠️ Retrying task: C
[Retry-Worker] ❌ Failed again. Moving to Second Retry Queue: C
[Second-Retry-Worker] ♻️ Retrying task (2nd attempt): C
[Second-Retry-Worker] ❌ Permanently Failed task C
```

---

## [Q5] What if we want to retry more? Is this scalable? Can you find out a better way for 5 retries?

**No, adding a new queue and thread for every retry count (Retry-3, Retry-4...) is NOT scalable.**

### Why?
1.  **Resource Waste**: Each queue needs a dedicated thread. If you want 10 retries, you need 10 extra threads per service, most of which will sit idle.
2.  **Complexity**: Monitoring and maintaining N queues is difficult.
3.  **Memory**: Queues take up heap space.

### Better Approach: Exponential Backoff with DLQ (Dead Letter Queue)

Instead of N queues, use **One Retry Queue** with a "Visibility Timeout" (or Delayed Message) feature.

**How it works (The Standard Pattern):**
1.  **Message Metadata**: Store `retry_count` inside the message/encapsulation.
2.  **Delay Mechanism**: When a task fails, increment `retry_count`.
    -   Calculate delay: `delay = initial_delay * 2^retry_count` (Exponential Backoff).
3.  **Re-enqueue with Delay**:
    -   **RabbitMQ**: Use `x-delay` exchange plugin.
    -   **AWS SQS**: Use `DelaySeconds`.
    -   **Redis/Java**: Use a `ScheduledExecutorService` or `HashedWheelTimer` to put the task back into the **SAME** retry queue after X seconds.
4.  **DLQ**: If `retry_count > MAX_RETRIES` (e.g., 5), move to Dead Letter Queue (DB/S3) for manual inspection.

**Architecture**:
`Main Queue` -> `Worker` (Fail) -> `Wait X sec` -> `Main Queue` (or single `Retry Queue`) -> `Worker` ... -> `DLQ`.

---

## [Q6] Idempotency: Find out how we can make tasks idempotent.

**Idempotency** means that performing an operation multiple times has the same result as performing it once. This is critical for retry systems where a task might be processed partially, fail, and then be processed again.

### Strategies to make tasks idempotent:

1.  **Unique Request ID (Deduplication)**:
    -   Assign a unique ID (UUID) to every task/message.
    -   Before processing, check a fast store (Redis) or DB: `IF exists(task_id) THEN skip`.
    -   **Example**: `INSERT (id, value) ... ON CONFLICT DO NOTHING`.

2.  **State Checking (conditional updates)**:
    -   Check the current state before applying changes.
    -   **Example**: `UPDATE account SET balance = balance - 100 WHERE id = 1 AND status = 'PENDING'`. (If status was already updated to 'PAID', the retry does nothing).

3.  **Result Storing**:
    -   Store the result of the calculation alongside the task status. If the task is retried, return the stored result instead of re-calculating (e.g., Charging a credit card).

---

## [Q7] Exponential Backoff Strategy: Implement delay of 1s, 2s, 4s...

We implemented **Exponential Backoff** using a `ScheduledExecutorService` to add delays before moving items to retry queues. This prevents "Cascading Failures" where a failing downstream service is hammered by immediate retries.

### Implementation Logic
```java
// Main Worker (Fail) -> Schedule Retry Queue in 1s
scheduler.schedule(() -> retryQueue.offer(task), 1, TimeUnit.SECONDS);

// Retry Worker (Fail) -> Schedule Second Retry Queue in 2s
scheduler.schedule(() -> secondRetryQueue.offer(task), 2, TimeUnit.SECONDS);
```

### Observed Behavior (Logs):
```
[Sequential-Worker] ❌ Error processing task C
[Sequential-Worker] 🕒 Scheduling retry in 1s -> Retry Queue: C
... (1 second delay) ...
[Retry-Worker] ♻️ Retrying task: C
[Retry-Worker] ❌ Failed again. Scheduling retry in 2s -> Second Retry Queue: C
... (2 seconds delay) ...
[Second-Retry-Worker] ♻️ Retrying task: C
```

---

## [Q8] Dead Letter Queue (DLQ): Create a new queue called dead-letter-queue and add the message there instead of discarding it.

We added a `deadLetterQueue` to `SequentialQueueService`.
-   When `Second-Retry-Worker` fails to process a task (after all retries are exhausted), it moves the task to `deadLetterQueue` instead of dropping it.
-   This queue serves as a holding area for messages that need manual intervention.

---

## [Q9] Alerting: Send an email for the messages present in the dead-letter queue. Which step would you do this?

**Decision**: We implemented a dedicated **DLQ Worker** (`processDeadLetterQueue`) to handle alerting.

### Why a separate worker?
-   **Decoupling**: Sending emails (or calling external APIs like Resend/SendGrid) is slow and network-bound. Doing this in the retry worker would block it from creating new DLQ entries.
-   **Batching**: A separate worker can read multiple messages and send a daily digest if needed (though we implemented immediate alerts for now).

### Implementation
-   **MockEmailService**: Created a mock service to simulate sending emails (logs to console).
-   **Flow**: `Main` -> `Retry` -> `Second Retry` -> `DLQ` -> `Email Alert`.

**Logs Observation:**
```
[Second-Retry-Worker] 💀 Moving to Dead Letter Queue (DLQ): C
[DLQ-Worker] 📩 Received dead letter: C
[MockEmailService] 📧 Sending Email Alert
   To: admin@example.com
   Subject: DLQ Alert: Task Failed
   Body: The following task failed all retries: C
```

---

## [Q10] Move everything to RabbitMQ. What was easy/hard compared to the above?

We migrated the entire flow to **RabbitMQ**.

### Architecture Changes
-   **Replaced**: `BlockingQueue` and `Thread` management with `RabbitTemplate` and `@RabbitListener`.
-   **Wait Queues with TTL**: Instead of `ScheduledExecutorService`, we used RabbitMQ's built-in **Time-To-Live (TTL)** and **Dead Letter Exchange (DLX)** pattern.
    -   `sequential.retry.wait`: Messages sit here for 1s, then are automatically routed to `sequential.retry.processing`.
    -   `sequential.second.retry.wait`: Messages sit here for 2s, then are automatically routed to `sequential.second.retry.processing`.

### Comparison

| Feature | Java In-Memory (BlockingQueue) | RabbitMQ |
| :--- | :--- | :--- |
| **Setup** | **Easy**. Just `new LinkedBlockingQueue()`. No infrastructure needed. | **Harder**. Need Docker/Brew install, dependency management (`spring-boot-starter-amqp`), and `RabbitConfig` to define queues/exchanges. |
| **Persistence** | **None**. If app restarts, messages are lost. | **Excellent**. Queues are durable. Messages survive app restarts. |
| **Retry Delay** | **Manual**. Had to use `ScheduledExecutorService` and complex thread management. | **Native**. `TTL` + `DLX` handles delays automatically without code. |
| **Scalability** | **Limited**. Bound to single JVM heap and CPU. | **High**. Can add multiple consumer instances (Microservices) to process the same queue. |
| **Observability**| **Poor**. Only `System.out.println`. | **Excellent**. RabbitMQ Management UI shows real-time graphs, message rates, and queue depths. |

### What was Hard?
1.  **Configuration**: Defining the precise binding logic (DLX, routing keys) in `RabbitMQConfig` is verbose compared to `queue.offer()`.
2.  **Debugging Startup**: If queues are not declared correctly (e.g., `Queue declaration failed`), the app crashes on startup. We had to ensure `RabbitAdmin` was configured to force declarations.

### Reference Logs (RabbitMQ)
```
[Sequential-Rabbit-Worker] ❌ Error processing task C
[Sequential-Rabbit-Worker] 🕒 Scheduling retry in 1s -> Retry Wait Queue
... (RabbitMQ handles 1s delay) ...
[Retry-Rabbit-Worker] ♻️ Retrying task: C
[Retry-Rabbit-Worker] ❌ Failed again. Scheduling retry in 2s -> Second Retry Wait Queue
... (RabbitMQ handles 2s delay) ...
[DLQ-Rabbit-Worker] 📩 Received dead letter: C
[MockEmailService] 📧 Sending Email Alert
```

---

## [Q11] Scheduled Emails: Calculate delay and add that much delay while adding task to queue.

We implemented **Scheduled Emails** functionality.

**Problem**: RabbitMQ does not support arbitrary scheduled messaging natively without the `rabbitmq-delayed-message-exchange` plugin, which was not available in this environment.

**Solution**: implemented the scheduling at the **Service Layer** using Java's `ScheduledExecutorService` to defer the *publishing* logic.

### Logic
1.  **Receive Request**: `POST /api/sequential/schedule-email` with a `scheduledTime` (ISO-8601).
2.  **Calculate Delay**: `delay = scheduledTime - LocalDateTime.now()`.
3.  **Defer Publish**:
    *   If `delay <= 0`: Publish to RabbitMQ immediately.
    *   If `delay > 0`: Use `scheduler.schedule(publishTask, delay)` to wait in memory.
4.  **Execute**: When delay expires, the scheduler thread publishes the message to the `sequential.queue` in RabbitMQ.

### Code Snippet
```java
long delay = Duration.between(LocalDateTime.now(), scheduledTime).toMillis();
scheduler.schedule(() -> {
    rabbitTemplate.convertAndSend("sequential.queue", emailDetails);
}, delay, TimeUnit.MILLISECONDS);
```

### Verification Logs
```
[SequentialService] 🕒 Scheduling email for 2026-02-08T11:39:51 (in 9558ms): Email to recipient@example.com
... (Wait 10s) ...
[SequentialService] ⏰ Delay expired. Enqueuing now: Email to recipient@example.com
[SequentialService] Enqueuing to RabbitMQ: Email to recipient@example.com
[Sequential-Rabbit-Worker] ▶ Starting task: Email to recipient@example.com
```
