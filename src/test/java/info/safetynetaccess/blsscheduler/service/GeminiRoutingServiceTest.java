package info.safetynetaccess.blsscheduler.service;

import info.safetynetaccess.blsscheduler.dto.AgentLogRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

public class GeminiRoutingServiceTest {

    @InjectMocks
    private GeminiRoutingService routingService;

    @Mock
    private AgentLoggingService loggingService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(routingService, "defaultModel", "Gemini 3.1 Flash-Lite");
        ReflectionTestUtils.setField(routingService, "escalatedModel", "Gemini 3.5 Flash");
    }

    @Test
    public void testEscalationCheck_SimplePrompts() {
        assertFalse(routingService.checkEscalationRequirement("How can I list classes?"));
        assertFalse(routingService.checkEscalationRequirement("What are the registered companies?"));
        assertFalse(routingService.checkEscalationRequirement(""));
        assertFalse(routingService.checkEscalationRequirement(null));
    }

    @Test
    public void testEscalationCheck_ComplexKeywords() {
        assertTrue(routingService.checkEscalationRequirement("Can you optimize this search routine?"));
        assertTrue(routingService.checkEscalationRequirement("Check for race condition and multithread issues"));
        assertTrue(routingService.checkEscalationRequirement("refactor the authentication model"));
    }

    @Test
    public void testEscalationCheck_CodeBlocks() {
        assertTrue(routingService.checkEscalationRequirement("Review the following logic: \n```\nSystem.out.println(\"test\");\n```"));
        assertTrue(routingService.checkEscalationRequirement("Explain public class App { public static void main }"));
        assertTrue(routingService.checkEscalationRequirement("SELECT * FROM companies WHERE approved = 'Yes'"));
    }

    @Test
    public void testRouteTask_SimpleRequest_DefaultsToFlashLite() {
        String prompt = "Who are the available instructors?";
        
        GeminiRoutingService.RoutingResult result = routingService.processAndRouteTask(prompt);

        assertNotNull(result);
        assertEquals("Gemini 3.1 Flash-Lite", result.getModelUsed());
        assertFalse(result.isEscalated());
        assertEquals("ROUTED", result.getStatus());
        assertTrue(result.getResponse().contains("LIGHTWEIGHT ROUTING"));

        // Verify logging integration
        ArgumentCaptor<AgentLogRequest> logCaptor = ArgumentCaptor.forClass(AgentLogRequest.class);
        Mockito.verify(loggingService, Mockito.times(1)).log(logCaptor.capture());
        
        AgentLogRequest loggedRequest = logCaptor.getValue();
        assertEquals("Gemini 3.1 Flash-Lite", loggedRequest.getModelUsed());
        assertEquals("INFO", loggedRequest.getSeverity());
        assertEquals("ROUTED", loggedRequest.getStatus());
    }

    @Test
    public void testRouteTask_ComplexRequest_EscalatesToFlash() {
        String prompt = "Write an optimized concurrent cache with TTL eviction in Java";
        
        GeminiRoutingService.RoutingResult result = routingService.processAndRouteTask(prompt);

        assertNotNull(result);
        assertEquals("Gemini 3.5 Flash", result.getModelUsed());
        assertTrue(result.isEscalated());
        assertEquals("SUCCESS_CANDIDATE", result.getStatus());
        assertTrue(result.getResponse().contains("ESCALATED DEEP REASONING"));
        assertTrue(result.getResponse().contains("OptimizedSolution"));

        // Verify logging integration
        ArgumentCaptor<AgentLogRequest> logCaptor = ArgumentCaptor.forClass(AgentLogRequest.class);
        Mockito.verify(loggingService, Mockito.times(1)).log(logCaptor.capture());
        
        AgentLogRequest loggedRequest = logCaptor.getValue();
        assertEquals("Gemini 3.5 Flash", loggedRequest.getModelUsed());
        assertEquals("INFO", loggedRequest.getSeverity());
        assertEquals("SUCCESS_CANDIDATE", loggedRequest.getStatus());
    }

    @Test
    public void testRouteTask_FailureCase_LogsError() {
        // Run with an empty prompt which triggers an IllegalArgumentException
        GeminiRoutingService.RoutingResult result = routingService.processAndRouteTask("   ");

        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getResponse().contains("routing workflow failed") || result.getResponse().contains("An error occurred"));

        // Verify error telemetry was logged
        ArgumentCaptor<AgentLogRequest> logCaptor = ArgumentCaptor.forClass(AgentLogRequest.class);
        Mockito.verify(loggingService, Mockito.times(1)).log(logCaptor.capture());
        
        AgentLogRequest loggedRequest = logCaptor.getValue();
        assertEquals("ERROR", loggedRequest.getSeverity());
        assertEquals("FAILED", loggedRequest.getStatus());
        assertTrue(loggedRequest.getMessage().contains("failed") || loggedRequest.getMessage().contains("Error"));
    }
}
