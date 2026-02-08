package com.url_shortner.project.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import com.url_shortner.project.dto.EmailRequest;
import com.url_shortner.project.service.impl.SequentialQueueService;
import java.util.List;

@RestController
@RequestMapping("/api/sequential")
public class SequentialController {

    private final SequentialQueueService sequentialQueueService;

    public SequentialController(SequentialQueueService sequentialQueueService) {
        this.sequentialQueueService = sequentialQueueService;
    }

    @PostMapping("/enqueue")
    public ResponseEntity<String> process(@RequestBody java.util.List<String> items) {
        for (String item : items) {
            sequentialQueueService.enqueue(item);
        }
        return ResponseEntity.ok("Enqueued " + items.size() + " items. Check logs for sequential processing.");
    }

    // [Q11] Scheduled Emails
    @PostMapping("/schedule-email")
    public ResponseEntity<String> scheduleEmail(@RequestBody EmailRequest request) {
        sequentialQueueService.scheduleEmail(request);
        return ResponseEntity.ok("Email scheduled for " + request.getScheduledTime());
    }
}
