package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.dto.AgentLogRequest;
import info.safetynetaccess.blsscheduler.service.AgentLoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller exposing a shared REST endpoint for Agent Platform logging.
 */
@RestController
@RequestMapping("/api/agent")
public class AgentLoggingController {

    @Autowired
    private AgentLoggingService loggingService;

    /**
     * POST /api/agent/log
     * Accepts structured agent log entries and buffers them for micro-batched ingestion.
     */
    @PostMapping("/log")
    public ResponseEntity<Map<String, String>> logEvent(@RequestBody AgentLogRequest request) {
        try {
            loggingService.log(request);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Log entry received and buffered for micro-batch ingestion.");
            response.put("logId", request.getLogId());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to buffer log: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * POST /api/agent/flush
     * Forces immediate flushing of the log buffer (useful for administration and automated testing).
     */
    @PostMapping("/flush")
    public ResponseEntity<Map<String, Object>> forceFlush() {
        int initialSize = loggingService.getBufferSize();
        loggingService.flushLogs();
        int finalSize = loggingService.getBufferSize();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Log buffer flush completed.");
        response.put("initialCount", initialSize);
        response.put("remainingCount", finalSize);
        
        return ResponseEntity.ok(response);
    }
}
