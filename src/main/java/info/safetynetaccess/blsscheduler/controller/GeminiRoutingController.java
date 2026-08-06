package info.safetynetaccess.blsscheduler.controller;
 
import info.safetynetaccess.blsscheduler.service.GeminiRoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
 
import java.util.HashMap;
import java.util.Map;
 
/**
 * Controller exposing REST endpoints to manage and test LLM workflow routing decisions, scoped by company.
 */
@RestController
@RequestMapping("/api/companies/{companyId}/agent")
public class GeminiRoutingController {
 
    @Autowired
    private GeminiRoutingService routingService;
 
    /**
     * POST /api/companies/{companyId}/agent/route
     * Evaluates a user prompt, determines the correct routing model, and returns the result.
     */
    @PostMapping("/route")
    public Mono<ResponseEntity<GeminiRoutingService.RoutingResult>> routeWorkflow(@PathVariable String companyId, @RequestBody Map<String, String> requestBody) {
        String prompt = requestBody.get("prompt");
        if (prompt == null || prompt.trim().isEmpty()) {
            GeminiRoutingService.RoutingResult errorResult = new GeminiRoutingService.RoutingResult();
            errorResult.setStatus("FAILED");
            errorResult.setResponse("Error: Required payload field 'prompt' is missing or empty.");
            return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResult));
        }
 
        // We can pass the companyId to the routing service if needed for company-specific context
        return Mono.fromCallable(() -> routingService.processAndRouteTask(prompt))
                .map(result -> {
                    if ("FAILED".equals(result.getStatus())) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                    }
                    return ResponseEntity.ok(result);
                })
                .onErrorResume(t -> {
                    GeminiRoutingService.RoutingResult errorResult = new GeminiRoutingService.RoutingResult();
                    errorResult.setStatus("FAILED");
                    errorResult.setResponse("Error: " + t.getMessage());
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResult));
                });
    }
}
