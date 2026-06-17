package info.safetynetaccess.blsscheduler.controller;

import info.safetynetaccess.blsscheduler.service.GeminiRoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller exposing REST endpoints to manage and test LLM workflow routing decisions.
 */
@RestController
@RequestMapping("/api/agent")
public class GeminiRoutingController {

    @Autowired
    private GeminiRoutingService routingService;

    /**
     * POST /api/agent/route
     * Evaluates a user prompt, determines the correct routing model (Lite vs full Flash), 
     * executes a simulated task, and returns the result and latency metadata.
     */
    @PostMapping("/route")
    public ResponseEntity<GeminiRoutingService.RoutingResult> routeWorkflow(@RequestBody Map<String, String> requestBody) {
        String prompt = requestBody.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            GeminiRoutingService.RoutingResult errorResult = new GeminiRoutingService.RoutingResult();
            errorResult.setStatus("FAILED");
            errorResult.setResponse("Error: Required payload field 'prompt' is missing or empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult);
        }

        GeminiRoutingService.RoutingResult result = routingService.processAndRouteTask(prompt);
        if ("FAILED".equals(result.getStatus())) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
        
        return ResponseEntity.ok(result);
    }
}
