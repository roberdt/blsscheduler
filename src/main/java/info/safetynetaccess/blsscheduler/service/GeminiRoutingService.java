package info.safetynetaccess.blsscheduler.service;

import info.safetynetaccess.blsscheduler.dto.AgentLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service to optimize internal LLM workflows.
 * Defaults to Gemini 3.1 Flash-Lite for routing, and escalates to Gemini 3.5 Flash
 * when deep code logic, architectural planning, or complex tasks require it.
 * Integrates with AgentLoggingService to record execution telemetry.
 */
@Service
public class GeminiRoutingService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiRoutingService.class);

    @Autowired
    private AgentLoggingService loggingService;

    @Value("${app.gemini.default-model:Gemini 3.1 Flash-Lite}")
    private String defaultModel;

    @Value("${app.gemini.escalated-model:Gemini 3.5 Flash}")
    private String escalatedModel;

    // Complex keywords/phrases that trigger escalation
    private static final List<String> ESCALATION_KEYWORDS = Arrays.asList(
            "complex", "deep logic", "architect", "security", "audit", "concurrency", 
            "multithread", "race condition", "deadlock", "optimize", "optimization", 
            "refactor", "restructure", "algorithmic", "big-o", "leak", "profile",
            "regex", "regular expression", "crypto", "encryption"
    );

    // Code-specific syntax elements that trigger escalation
    private static final List<String> CODE_INDICATORS = Arrays.asList(
            "class ", "public class", "private class", "public void", "implements", 
            "extends", "interface ", "@Override", "fn ", "def ", "lambda", "sql", 
            "select ", "insert ", "update ", "delete ", "where ", "join "
    );

    /**
     * Determines whether a given task warrants model escalation.
     * Escalates to Gemini 3.5 Flash if the score based on heuristics exceeds threshold.
     */
    public boolean checkEscalationRequirement(String taskDescription) {
        if (taskDescription == null || taskDescription.trim().isEmpty()) {
            return false;
        }

        String normalizedTask = taskDescription.toLowerCase();

        // Count occurrences or matching elements
        long keywordMatches = ESCALATION_KEYWORDS.stream()
                .filter(normalizedTask::contains)
                .count();

        long codeMatches = CODE_INDICATORS.stream()
                .filter(normalizedTask::contains)
                .count();

        boolean hasCodeBlock = normalizedTask.contains("```");

        logger.debug("Routing evaluation - Keyword matches: {}, Code matches: {}, Has code block: {}", 
                keywordMatches, codeMatches, hasCodeBlock);

        // Escalation threshold logic: any keyword match, code match, or structural code block escalates the task
        return keywordMatches > 0 || codeMatches > 0 || hasCodeBlock;
    }

    /**
     * Executes the routing workflow.
     * Assigns the request to the correct model, simulates the invocation, 
     * records latency, and sends structured telemetry to the shared AgentLoggingService.
     */
    public RoutingResult processAndRouteTask(String userPrompt) {
        long startTime = System.currentTimeMillis();
        String logId = UUID.randomUUID().toString();
        
        RoutingResult result = new RoutingResult();
        result.setLogId(logId);
        result.setUserPrompt(userPrompt);

        // 1. Evaluate complexity and choose model
        boolean requiresEscalation = checkEscalationRequirement(userPrompt);
        String selectedModel = requiresEscalation ? escalatedModel : defaultModel;
        result.setModelUsed(selectedModel);
        result.setEscalated(requiresEscalation);

        try {
            // 2. Simulate task execution & AI response generation
            if (userPrompt == null || userPrompt.trim().isEmpty()) {
                throw new IllegalArgumentException("User prompt cannot be null or empty.");
            }

            // Simulate slight execution delay to model latency (Flash-Lite is faster, Flash takes more time)
            long simulationDelay = requiresEscalation ? 150 : 40;
            Thread.sleep(simulationDelay);

            String aiResponse = generateSimulatedResponse(userPrompt, selectedModel, requiresEscalation);
            result.setResponse(aiResponse);
            
            // Determine if code generation was successful (simulate SUCCESS_CANDIDATE status)
            boolean isCodeGeneration = userPrompt.toLowerCase().contains("code") || userPrompt.toLowerCase().contains("write") || requiresEscalation;
            String status = isCodeGeneration ? "SUCCESS_CANDIDATE" : "ROUTED";
            result.setStatus(status);

            long latency = System.currentTimeMillis() - startTime;
            result.setLatencyMs(latency);

            // 3. Queue telemetry logs
            AgentLogRequest logRequest = new AgentLogRequest();
            logRequest.setLogId(logId);
            logRequest.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            logRequest.setSeverity("INFO");
            logRequest.setMessage(String.format("Successfully routed and processed task using %s.", selectedModel));
            logRequest.setStatus(status);
            logRequest.setModelUsed(selectedModel);
            logRequest.setLatencyMs(latency);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("escalated", requiresEscalation);
            metadata.put("promptLength", userPrompt.length());
            metadata.put("codeGeneration", isCodeGeneration);
            logRequest.setMetadata(metadata);

            loggingService.log(logRequest);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            result.setLatencyMs(latency);
            result.setStatus("FAILED");
            result.setResponse("An error occurred during routing workflow: " + e.getMessage());

            // Queue error telemetry logs (these should bypass exclusion filters and stream to BigQuery)
            AgentLogRequest errorLog = new AgentLogRequest();
            errorLog.setLogId(logId);
            errorLog.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            errorLog.setSeverity("ERROR");
            errorLog.setMessage(String.format("Routing workflow failed for model %s. Error: %s", selectedModel, e.getMessage()));
            errorLog.setStatus("FAILED");
            errorLog.setModelUsed(selectedModel);
            errorLog.setLatencyMs(latency);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("errorType", e.getClass().getSimpleName());
            metadata.put("promptPreview", userPrompt != null && userPrompt.length() > 50 ? userPrompt.substring(0, 50) + "..." : userPrompt);
            errorLog.setMetadata(metadata);

            loggingService.log(errorLog);
        }

        return result;
    }

    /**
     * Generates standard high-quality boilerplate mock code and responses mimicking model behaviors.
     */
    private String generateSimulatedResponse(String prompt, String model, boolean escalated) {
        if (escalated) {
            return String.format("[Response generated by %s - ESCALATED DEEP REASONING]\n\n" +
                    "Evaluating deep logic requirements for prompt: '%s'\n\n" +
                    "Here is the optimized robust solution:\n" +
                    "```java\n" +
                    "// Highly performant solution utilizing concurrency controls\n" +
                    "public class OptimizedSolution {\n" +
                    "    private final Map<String, Object> cache = new java.util.concurrent.ConcurrentHashMap<>();\n" +
                    "    \n" +
                    "    public Object getOrCompute(String key, java.util.function.Function<String, Object> computer) {\n" +
                    "        return cache.computeIfAbsent(key, computer);\n" +
                    "    }\n" +
                    "}\n" +
                    "```\n" +
                    "Execution analysis reveals optimal Time Complexity O(1), Space Complexity O(N).", model, prompt);
        } else {
            return String.format("[Response generated by %s - LIGHTWEIGHT ROUTING]\n\n" +
                    "Standard response to instruction: '%s'\n\n" +
                    "Task successfully processed with minimal latency and standard model orchestration.", model, prompt);
        }
    }

    // Response mapping class
    public static class RoutingResult {
        private String logId;
        private String modelUsed;
        private boolean escalated;
        private String status;
        private String response;
        private long latencyMs;
        private String userPrompt;

        public String getLogId() { return logId; }
        public void setLogId(String logId) { this.logId = logId; }

        public String getModelUsed() { return modelUsed; }
        public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

        public boolean isEscalated() { return escalated; }
        public void setEscalated(boolean escalated) { this.escalated = escalated; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }

        public long getLatencyMs() { return latencyMs; }
        public void setLatencyMs(long latencyMs) { this.latencyMs = latencyMs; }

        public String getUserPrompt() { return userPrompt; }
        public void setUserPrompt(String userPrompt) { this.userPrompt = userPrompt; }
    }
}
