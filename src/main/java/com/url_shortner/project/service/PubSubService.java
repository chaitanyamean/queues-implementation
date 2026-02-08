package com.url_shortner.project.service;

import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

@Service
public class PubSubService {

    // Thread-safe map to hold subscribers for each event type
    private final Map<String, List<Consumer<Object>>> subscribers = new ConcurrentHashMap<>();

    // Executor pool for handling events asynchronously
    // Using CachedThreadPool to spawn threads as needed (similar to the previous
    // behaviors if load is high)
    // or FixedThreadPool if we wanted to limit concurrency.
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * Subscribe to an event with a callback function.
     * 
     * @param eventType The name of the event (e.g., "IMAGE_UPLOADED")
     * @param listener  The function to execute when the event fires
     */
    public void subscribe(String eventType, Consumer<Object> listener) {
        subscribers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Publish an event to all subscribers.
     * 
     * @param eventType The name of the event
     * @param data      The data payload to pass to subscribers
     */
    public void publish(String eventType, Object data) {
        List<Consumer<Object>> listeners = subscribers.get(eventType);

        if (listeners == null || listeners.isEmpty()) {
            System.out.println("[PubSub] No subscribers for event: " + eventType);
            return;
        }

        System.out.println("[PubSub] Publishing event '" + eventType + "' to " + listeners.size() + " subscribers.");

        for (Consumer<Object> listener : listeners) {
            // Execute each listener in a separate thread (or pool) so they run in parallel
            executorService.submit(() -> {
                try {
                    listener.accept(data);
                } catch (Exception e) {
                    System.err.println("[PubSub] Error in subscriber execution: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}
