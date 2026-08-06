package info.safetynetaccess.blsscheduler.controller;
 
import info.safetynetaccess.blsscheduler.dto.AgentLogRequest;
import info.safetynetaccess.blsscheduler.service.AgentLoggingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Controller exposing a shared REST endpoint for Agent Platform logging, scoped by company.
 */
@RestController
@RequestMapping("/api/companies/{companyId}/agent")
public class AgentLoggingController {
 
    @Autowired
    private AgentLoggingService loggingService;
 
    /**
     * POST /api/companies/{companyId}/agent/log
     * Accepts structured agent log entries and buffers them for micro-batched ingestion.
     */
    @PostMapping("/log")
    public Mono<ResponseEntity<Map<String, String>>> logEvent(@PathVariable String companyId, @RequestBody AgentLogRequest request) {
        return Mono.fromCallable(() -> {
            // We can optionally attach the companyId to the metadata or log entry here
            if (request.getMetadata() == null) {
                request.setMetadata(new HashMap<>());
            }
            request.getMetadata().put("companyId", companyId);
            
            loggingService.log(request);
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Log entry received and buffered for micro-batch ingestion.");
            response.put("logId", request.getLogId());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        }).onErrorResume(t -> {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("status", "ERROR");
            errorResponse.put("message", "Failed to buffer log: " + t.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse));
        });
    }
 
    /**
     * POST /api/companies/{companyId}/agent/flush
     * Forces immediate flushing of the log buffer.
     */
    @PostMapping("/flush")
    public Mono<ResponseEntity<Map<String, Object>>> forceFlush(@PathVariable String companyId) {
        return Mono.fromCallable(() -> {
            int initialSize = loggingService.getBufferSize();
            loggingService.flushLogs();
            int finalSize = loggingService.getBufferSize();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("message", "Log buffer flush completed.");
            response.put("initialCount", initialSize);
            response.put("remainingCount", finalSize);
            
            return ResponseEntity.ok(response);
        });
    }
}
