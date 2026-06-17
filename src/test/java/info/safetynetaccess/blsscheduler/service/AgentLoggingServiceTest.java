package info.safetynetaccess.blsscheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import info.safetynetaccess.blsscheduler.dto.AgentLogRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

public class AgentLoggingServiceTest {

    @InjectMocks
    private AgentLoggingService loggingService;

    @Mock
    private Storage storage;

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Configure test parameters
        ReflectionTestUtils.setField(loggingService, "localLogDir", tempDir.toAbsolutePath().toString());
        ReflectionTestUtils.setField(loggingService, "gcsBucketName", "test-bucket");
    }

    @Test
    public void testLogAddsToBufferAndGeneratesMetadata() {
        loggingService.clearBuffer();
        assertEquals(0, loggingService.getBufferSize());

        AgentLogRequest req = new AgentLogRequest();
        req.setSeverity("INFO");
        req.setMessage("Test Message");
        req.setStatus("SUCCESS");

        loggingService.log(req);

        assertEquals(1, loggingService.getBufferSize());
        assertNotNull(req.getLogId());
        assertNotNull(req.getTimestamp());
    }

    @Test
    public void testFlushLogsLocalFallback() throws IOException {
        loggingService.clearBuffer();
        ReflectionTestUtils.setField(loggingService, "activeProfile", "local");

        AgentLogRequest req1 = new AgentLogRequest("id-1", "2026-06-10T12:00:00", "INFO", "Message 1", "ROUTED", "Model-A", 10, new HashMap<>());
        AgentLogRequest req2 = new AgentLogRequest("id-2", "2026-06-10T12:01:00", "ERROR", "Message 2", "FAILED", "Model-B", 25, new HashMap<>());

        loggingService.log(req1);
        loggingService.log(req2);

        assertEquals(2, loggingService.getBufferSize());

        // Perform flush (which should write locally since activeProfile = local)
        loggingService.flushLogs();

        assertEquals(0, loggingService.getBufferSize()); // Buffer must be drained

        // Verify local file creation
        try (Stream<Path> walk = Files.walk(tempDir)) {
            List<Path> files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
            assertEquals(1, files.size(), "Should have created exactly one log file");
            
            Path logFile = files.get(0);
            assertTrue(logFile.getFileName().toString().endsWith(".jsonl"));

            // Read lines and verify content
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            assertEquals(2, lines.size());

            AgentLogRequest log1 = objectMapper.readValue(lines.get(0), AgentLogRequest.class);
            AgentLogRequest log2 = objectMapper.readValue(lines.get(1), AgentLogRequest.class);

            assertEquals("id-1", log1.getLogId());
            assertEquals("Message 1", log1.getMessage());
            assertEquals("id-2", log2.getLogId());
            assertEquals("ERROR", log2.getSeverity());
        }
    }

    @Test
    public void testFlushLogsToGCS() {
        loggingService.clearBuffer();
        ReflectionTestUtils.setField(loggingService, "activeProfile", "prod");

        // Set up storage mock to return successfully on creation
        Mockito.when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        AgentLogRequest req = new AgentLogRequest("id-gcs", "2026-06-10T12:00:00", "INFO", "GCS message", "ROUTED", "Model-A", 5, new HashMap<>());
        loggingService.log(req);

        // Perform flush (activeProfile is NOT local, and storage is mocked)
        loggingService.flushLogs();

        assertEquals(0, loggingService.getBufferSize());

        // Verify that storage.create was called
        Mockito.verify(storage, Mockito.times(1)).create(any(BlobInfo.class), any(byte[].class));
    }
}
