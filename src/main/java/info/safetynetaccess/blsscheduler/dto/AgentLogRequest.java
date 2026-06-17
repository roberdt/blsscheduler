package info.safetynetaccess.blsscheduler.dto;

import java.util.Map;

/**
 * Structured request DTO for Agent Platform endpoint logging.
 */
public class AgentLogRequest {
    private String logId;
    private String timestamp;
    private String severity; // INFO, WARN, ERROR
    private String message;
    private String status;   // SUCCESS_CANDIDATE, ROUTED, ESCALATED, FAILED
    private String modelUsed;// Gemini 3.1 Flash-Lite, Gemini 3.5 Flash
    private long latencyMs;
    private Map<String, Object> metadata;

    // Default constructor
    public AgentLogRequest() {}

    public AgentLogRequest(String logId, String timestamp, String severity, String message, 
                           String status, String modelUsed, long latencyMs, Map<String, Object> metadata) {
        this.logId = logId;
        this.timestamp = timestamp;
        this.severity = severity;
        this.message = message;
        this.status = status;
        this.modelUsed = modelUsed;
        this.latencyMs = latencyMs;
        this.metadata = metadata;
    }

    // Getters and Setters
    public String getLogId() {
        return logId;
    }

    public void setLogId(String logId) {
        this.logId = logId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "AgentLogRequest{" +
                "logId='" + logId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                ", severity='" + severity + '\'' +
                ", message='" + message + '\'' +
                ", status='" + status + '\'' +
                ", modelUsed='" + modelUsed + '\'' +
                ", latencyMs=" + latencyMs +
                ", metadata=" + metadata +
                '}';
    }
}
