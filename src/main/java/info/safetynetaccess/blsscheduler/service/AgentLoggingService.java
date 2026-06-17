package info.safetynetaccess.blsscheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import info.safetynetaccess.blsscheduler.dto.AgentLogRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Service for capturing, buffering, and micro-batching Agent Platform logs.
 * Supports exporting to Google Cloud Storage (GCS) for free BigQuery loading,
 * with local disk fallback for developer machines and testing environments.
 */
@Service
public class AgentLoggingService {

    private static final Logger logger = LoggerFactory.getLogger(AgentLoggingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // Buffer to hold log entries in memory
    private final ConcurrentLinkedQueue<AgentLogRequest> logBuffer = new ConcurrentLinkedQueue<>();

    @Autowired(required = false)
    private Storage storage;

    @Value("${app.logging.gcs-bucket:bls-scheduler-agent-logs}")
    private String gcsBucketName;

    @Value("${app.logging.local-dir:logs/micro-batches}")
    private String localLogDir;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    /**
     * Submit an Agent Platform log entry into the buffer.
     * Also outputs a structured JSON log message to standard output immediately.
     */
    public void log(AgentLogRequest logRequest) {
        if (logRequest.getLogId() == null) {
            logRequest.setLogId(UUID.randomUUID().toString());
        }
        if (logRequest.getTimestamp() == null) {
            logRequest.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        }

        // Add to the micro-batch in-memory buffer
        logBuffer.add(logRequest);

        // Immediate structured stdout logging (Standard GCP Log ingestion / Cloud Logging)
        try {
            // Include logType flag so that GCL Log Router Sink can filter selectively
            String jsonLog = objectMapper.writeValueAsString(logRequest);
            // Prefixing/injecting logType in a structured payload
            String structuredLog = String.format("{\"logType\":\"AGENT_PLATFORM\",\"severity\":\"%s\",\"payload\":%s}",
                    logRequest.getSeverity(), jsonLog);
            
            if ("ERROR".equalsIgnoreCase(logRequest.getSeverity())) {
                logger.error(structuredLog);
            } else if ("WARN".equalsIgnoreCase(logRequest.getSeverity())) {
                logger.warn(structuredLog);
            } else {
                logger.info(structuredLog);
            }
        } catch (Exception e) {
            logger.error("Failed to write immediate structured log: {}", e.getMessage());
            logger.info("AgentLog: {}", logRequest);
        }
    }

    /**
     * Periodically flushes the buffered log entries to GCS or Local Disk.
     * Scheduled to run every 30 seconds.
     */
    @Scheduled(fixedRateString = "${app.logging.flush-interval-ms:30000}")
    public void flushLogs() {
        if (logBuffer.isEmpty()) {
            return;
        }

        logger.debug("Starting micro-batch log flush. Buffered entries count: {}", logBuffer.size());

        // Drain the queue into a temporary list
        List<AgentLogRequest> logsToFlush = new ArrayList<>();
        AgentLogRequest request;
        while ((request = logBuffer.poll()) != null) {
            logsToFlush.add(request);
        }

        if (logsToFlush.isEmpty()) {
            return;
        }

        // Serialize all entries as a JSONL (JSON Lines) string
        StringBuilder jsonlBuilder = new StringBuilder();
        for (AgentLogRequest item : logsToFlush) {
            try {
                jsonlBuilder.append(objectMapper.writeValueAsString(item)).append("\n");
            } catch (IOException e) {
                logger.error("Error serializing log item with ID {}: {}", item.getLogId(), e.getMessage());
            }
        }

        String jsonlData = jsonlBuilder.toString();
        if (jsonlData.trim().isEmpty()) {
            return;
        }

        // Generate filename based on date and time
        LocalDateTime now = LocalDateTime.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String timeStamp = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        String fileName = String.format("agent_logs_%s_%s.jsonl", timeStamp, UUID.randomUUID().toString().substring(0, 8));

        // Attempt upload to Google Cloud Storage
        boolean uploaded = false;
        if (storage != null && !"local".equalsIgnoreCase(activeProfile)) {
            try {
                String blobName = String.format("year=%04d/month=%02d/day=%02d/%s", 
                        now.getYear(), now.getMonthValue(), now.getDayOfMonth(), fileName);
                
                BlobId blobId = BlobId.of(gcsBucketName, blobName);
                BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                        .setContentType("application/x-ndjson")
                        .build();

                storage.create(blobInfo, jsonlData.getBytes(StandardCharsets.UTF_8));
                logger.info("Successfully uploaded micro-batch logs of size {} to GCS bucket: gs://{}/{}", 
                        logsToFlush.size(), gcsBucketName, blobName);
                uploaded = true;
            } catch (Exception e) {
                logger.error("Failed to upload micro-batch logs to GCS. Falling back to local disk storage: {}", e.getMessage());
            }
        }

        // Local disk fallback (if GCS is disabled, unavailable, or on local profile)
        if (!uploaded) {
            writeLogsToLocalDisk(datePath, fileName, jsonlData, logsToFlush.size());
        }
    }

    private void writeLogsToLocalDisk(String datePath, String fileName, String content, int recordCount) {
        try {
            Path targetDir = Paths.get(localLogDir, datePath);
            Files.createDirectories(targetDir);

            Path targetFile = targetDir.resolve(fileName);
            Files.write(targetFile, content.getBytes(StandardCharsets.UTF_8), 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("Successfully wrote micro-batch logs of size {} to local storage: {}", 
                    recordCount, targetFile.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Critical: Failed to save micro-batch logs to local storage. Logs are lost. Error: {}", e.getMessage());
        }
    }

    // Helper method for testing/inspection
    public int getBufferSize() {
        return logBuffer.size();
    }

    // Helper method to clear the buffer manually
    public void clearBuffer() {
        logBuffer.clear();
    }
}
