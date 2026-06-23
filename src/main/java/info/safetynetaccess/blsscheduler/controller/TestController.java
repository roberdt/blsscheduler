package info.safetynetaccess.blsscheduler.controller;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import info.safetynetaccess.blsscheduler.model.ClassSlot;
import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.model.Date;
import info.safetynetaccess.blsscheduler.model.Time;
import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import info.safetynetaccess.blsscheduler.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Test Controller for verifying BLS Scheduler system integrity.
 * Exposes endpoints to run full programmatic integration, unit, and E2E tests against Firestore and JWT services.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private Firestore firestore;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private ClassSlotController classSlotController;

    /**
     * Helper utility to convert a Google ApiFuture into a Reactor Mono.
     */
    private static <T> Mono<T> fromApiFuture(ApiFuture<T> future) {
        return Mono.create(sink -> ApiFutures.addCallback(future, new ApiFutureCallback<T>() {
            @Override
            public void onFailure(Throwable t) {
                sink.error(t);
            }

            @Override
            public void onSuccess(T result) {
                sink.success(result);
            }
        }, MoreExecutors.directExecutor()));
    }

    /**
     * GET /api/test/run
     * Runs programmatic system verification tests and returns a premium visual report.
     */
    @GetMapping("/run")
    public Mono<ResponseEntity<TestReport>> runAllTests() {
        TestReport report = new TestReport("BLS Scheduler Verification Suite");
        long suiteStart = System.currentTimeMillis();

        // 1. Run JWT & Authentication Service Tests (Synchronous)
        runJwtServiceTest(report);

        // 2. Run controller validation tests & E2E integration test (Reactive/Async)
        return runE2EAndValidationTests(report)
                .map(updatedReport -> {
                    updatedReport.setTotalDurationMs(System.currentTimeMillis() - suiteStart);
                    boolean allPassed = updatedReport.getResults().stream()
                            .allMatch(res -> "PASSED".equals(res.getStatus()));
                    updatedReport.setStatus(allPassed ? "PASSED" : "FAILED");
                    return ResponseEntity.ok(updatedReport);
                })
                .onErrorResume(t -> {
                    TestResult failResult = new TestResult("E2E Integration Test Runner Failure");
                    failResult.setStatus("FAILED");
                    failResult.setDetails("Test suite runner encountered critical error: " + t.getMessage());
                    report.addResult(failResult);
                    report.setStatus("FAILED");
                    report.setTotalDurationMs(System.currentTimeMillis() - suiteStart);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(report));
                });
    }

    /**
     * Test 1: Programmatically tests the JWT and UserDetails services.
     */
    private void runJwtServiceTest(TestReport report) {
        TestResult result = new TestResult("JWT Authentication Flow Integrity");
        long start = System.currentTimeMillis();
        try {
            // Retrieve default user details
            UserDetails userDetails = userDetailsService.loadUserByUsername("user");
            if (userDetails == null) {
                throw new IllegalStateException("UserDetails for 'user' returned null");
            }

            // Generate JWT Token
            String token = jwtUtil.generateToken(userDetails);
            if (token == null || token.isEmpty()) {
                throw new IllegalStateException("Generated JWT token is null or empty");
            }

            // Extract username and validate signature
            String username = jwtUtil.extractUsername(token);
            if (!"user".equals(username)) {
                throw new IllegalStateException("Extracted username '" + username + "' does not match 'user'");
            }

            boolean isValid = jwtUtil.validateToken(token, userDetails);
            if (!isValid) {
                throw new IllegalStateException("Generated token failed signature or expiration validation");
            }

            result.setStatus("PASSED");
            result.setDetails("Successfully verified default user loading, JWT generation, signature integrity, claims extraction, and token validation.");
        } catch (Exception e) {
            result.setStatus("FAILED");
            result.setDetails("JWT verification failed: " + e.getMessage());
        } finally {
            result.setDurationMs(System.currentTimeMillis() - start);
            report.addResult(result);
        }
    }

    /**
     * Test 2 & 3: Run class slot controller input bounds validation and E2E Firestore Integration test.
     */
    private Mono<TestReport> runE2EAndValidationTests(TestReport report) {
        TestResult validationResult = new TestResult("Request Input Validation Limits");
        TestResult e2eResult = new TestResult("End-to-End Firestore Integration & Pre-population");

        long validationStart = System.currentTimeMillis();
        
        // Setup mock user Principal
        Principal mockPrincipal = () -> "user";
        String mockCompanyId = "test-company-" + UUID.randomUUID().toString().substring(0, 8);

        // Verify Validation Limits on ClassSlotController
        ClassSlotController.DateScheduleRequest badRequest = new ClassSlotController.DateScheduleRequest();
        badRequest.setDateValue("2026-06-30");
        badRequest.setStarttime("1:00 pm");
        badRequest.setEndtime("1:45 pm");
        badRequest.setNumberslots(0); // 0 is invalid

        return classSlotController.createDateSchedule(mockCompanyId, mockPrincipal, badRequest)
                .flatMap(validationResponse -> {
                    long validationDuration = System.currentTimeMillis() - validationStart;
                    validationResult.setDurationMs(validationDuration);

                    if (validationResponse.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        validationResult.setStatus("PASSED");
                        validationResult.setDetails("Successfully verified boundary limits. Controller rejected request with 0 slots with status '400 Bad Request'.");
                    } else {
                        validationResult.setStatus("FAILED");
                        validationResult.setDetails("Boundary limit failure. Controller returned status '" + validationResponse.getStatusCode() + "' instead of expected '400 Bad Request' for 0 slots.");
                    }
                    report.addResult(validationResult);

                    // Now start the massive E2E Firestore integration check
                    long e2eStart = System.currentTimeMillis();

                    // Step A: Create an approved test company in Firestore
                    Company testCompany = new Company("Test Auto-Verifier Inc.", "Validating E2E system health and pre-population", "Yes");
                    DocumentReference companyDocRef = firestore.collection("companies").document(mockCompanyId);

                    return fromApiFuture(companyDocRef.set(testCompany))
                            .flatMap(unused -> {
                                // Step B: Request scheduling of a date, time, and 3 slots
                                ClassSlotController.DateScheduleRequest validRequest = new ClassSlotController.DateScheduleRequest();
                                validRequest.setDateValue("2026-06-30");
                                validRequest.setStarttime("1:00 pm");
                                validRequest.setEndtime("1:45 pm");
                                validRequest.setNumberslots(3);
                                validRequest.setApproved(false);
                                validRequest.setCoursetitle("E2E Test Course");
                                validRequest.setCoursetype("BLS Provider");
                                validRequest.setInstructorname("Automated Tester");

                                return classSlotController.createDateSchedule(mockCompanyId, mockPrincipal, validRequest);
                            })
                            .flatMap(scheduleResponse -> {
                                if (scheduleResponse.getStatusCode() != HttpStatus.CREATED) {
                                    throw new IllegalStateException("Failed to create date schedule. Response status was: " + scheduleResponse.getStatusCode());
                                }

                                ClassSlotController.SuccessResponse body = (ClassSlotController.SuccessResponse) scheduleResponse.getBody();
                                if (body == null) {
                                    throw new IllegalStateException("Success response body was null");
                                }

                                String dateId = body.getDateId();
                                String timeId = body.getTimeId();
                                List<ClassSlot> returnedSlots = body.getClassSlots();

                                // Step C: Verify 3 pre-populated class slots in response
                                if (returnedSlots.size() != 3) {
                                    throw new IllegalStateException("Expected exactly 3 class slots in response, but got: " + returnedSlots.size());
                                }

                                boolean allPrepopulated = returnedSlots.stream()
                                        .allMatch(slot -> "No Show".equals(slot.getAttendance()) && 
                                                          "".equals(slot.getFirstname()) && 
                                                          "".equals(slot.getLastname()) &&
                                                          "".equals(slot.getPostnominalcredential()) &&
                                                          "No Grade".equals(slot.getPassfail()));
                                if (!allPrepopulated) {
                                    throw new IllegalStateException("Class slots were not correctly pre-populated with empty names or No Show attendance");
                                }

                                // Step D: Fetch from Firestore to confirm they persist correctly
                                DocumentReference timeRef = companyDocRef.collection("dates").document(dateId).collection("times").document(timeId);
                                return fromApiFuture(timeRef.get())
                                        .flatMap(timeDoc -> {
                                            if (!timeDoc.exists()) {
                                                throw new IllegalStateException("Time document does not exist in Firestore.");
                                            }
                                            Time savedTime = timeDoc.toObject(Time.class);
                                            if (savedTime.isApproved() ||
                                                !"E2E Test Course".equals(savedTime.getCoursetitle()) ||
                                                !"BLS Provider".equals(savedTime.getCoursetype()) ||
                                                !"Automated Tester".equals(savedTime.getInstructorname())) {
                                                throw new IllegalStateException("Saved Time document fields do not match request parameters.");
                                            }
                                            return fromApiFuture(timeRef.collection("classslots").get());
                                        })
                                        .flatMap(querySnapshot -> {
                                            if (querySnapshot.size() != 3) {
                                                throw new IllegalStateException("Firestore subcollection contains " + querySnapshot.size() + " slots, expected exactly 3.");
                                            }

                                            // Step E: Pick first class slot and update/book it
                                            QueryDocumentSnapshot firstSlotDoc = querySnapshot.getDocuments().get(0);
                                            String slotId = firstSlotDoc.getId();

                                            ClassSlot updateReq = new ClassSlot();
                                            updateReq.setFirstname("John");
                                            updateReq.setLastname("Doe");
                                            updateReq.setPostnominalcredential("RN, MSN");
                                            updateReq.setAttendance("Show");
                                            updateReq.setPassfail("Pass");
                                            updateReq.setScore("95");

                                            return classSlotController.updateClassSlot(mockCompanyId, dateId, timeId, slotId, updateReq)
                                                    .flatMap(updateResponse -> {
                                                        // Step E2: Verify "Get All Classes" endpoint
                                                        TestResult allClassesTestResult = new TestResult("All-Classes Retrieval Integration");
                                                        long allClassesStart = System.currentTimeMillis();
                                                        return classSlotController.getAllClasses(mockCompanyId)
                                                                .flatMap(allClassesResponse -> {
                                                                    allClassesTestResult.setDurationMs(System.currentTimeMillis() - allClassesStart);
                                                                    if (allClassesResponse.getStatusCode() != HttpStatus.OK) {
                                                                        allClassesTestResult.setStatus("FAILED");
                                                                        allClassesTestResult.setDetails("Failed to fetch all classes. Status: " + allClassesResponse.getStatusCode());
                                                                    } else {
                                                                        List<ClassSlotController.CompanyClassResponse> classes = (List<ClassSlotController.CompanyClassResponse>) allClassesResponse.getBody();
                                                                        if (classes == null || classes.isEmpty()) {
                                                                            allClassesTestResult.setStatus("FAILED");
                                                                            allClassesTestResult.setDetails("All-classes endpoint returned empty list, expected at least 1 session.");
                                                                        } else {
                                                                            boolean foundCreated = classes.stream().anyMatch(c -> c.getDateId().equals(dateId) && c.getClassInfo().getId().equals(timeId));
                                                                            if (foundCreated) {
                                                                                allClassesTestResult.setStatus("PASSED");
                                                                                allClassesTestResult.setDetails("Successfully verified 'all-classes' endpoint. Found the newly created session across date boundaries.");
                                                                            } else {
                                                                                allClassesTestResult.setStatus("FAILED");
                                                                                allClassesTestResult.setDetails("Created session was not found in the 'all-classes' response.");
                                                                            }
                                                                        }
                                                                    }
                                                                    report.addResult(allClassesTestResult);
                                                                    return Mono.just(updateResponse);
                                                                });
                                                    })
                                                    .flatMap(updateResponse -> {
                                                        if (updateResponse.getStatusCode() != HttpStatus.OK) {
                                                            throw new IllegalStateException("Failed to update class slot. Response status: " + updateResponse.getStatusCode());
                                                        }

                                                        ClassSlot updatedSlot = (ClassSlot) updateResponse.getBody();
                                                        if (updatedSlot == null || !"John".equals(updatedSlot.getFirstname()) || !"Show".equals(updatedSlot.getAttendance())) {
                                                            throw new IllegalStateException("Updated slot returned from endpoint did not match update request parameters.");
                                                        }

                                                        // Step F: Cleanup test records bottom-up
                                                        List<Mono<WriteResult>> deletes = new ArrayList<>();
                                                        for (QueryDocumentSnapshot doc : querySnapshot.getDocuments()) {
                                                            deletes.add(fromApiFuture(doc.getReference().delete()));
                                                        }

                                                        return Flux.concat(deletes).then()
                                                                .then(fromApiFuture(timeRef.delete()))
                                                                .then(fromApiFuture(companyDocRef.collection("dates").document(dateId).delete()))
                                                                .then(fromApiFuture(companyDocRef.delete()))
                                                                .then(Mono.defer(() -> {
                                                                    e2eResult.setStatus("PASSED");
                                                                    e2eResult.setDetails("All E2E checks passed: (1) Created Mock Company, (2) Scheduled Class Date & Time, (3) Generated exactly 3 pre-populated ClassSlots with 'NA' attendance, (4) Updated and booked a slot, and (5) Completed bottom-up cleanup successfully.");
                                                                    e2eResult.setDurationMs(System.currentTimeMillis() - e2eStart);
                                                                    report.addResult(e2eResult);
                                                                    return Mono.just(report);
                                                                }));
                                                    });
                                        });
                            })
                            // Exception handler to ensure cleanup occurs even if tests fail midway
                            .onErrorResume(e -> {
                                e2eResult.setStatus("FAILED");
                                e2eResult.setDetails("E2E database integration failed: " + e.getMessage() + ". Attempting emergency resource cleanup.");
                                e2eResult.setDurationMs(System.currentTimeMillis() - e2eStart);
                                report.addResult(e2eResult);

                                // Perform emergency cleanup of the created company document
                                return fromApiFuture(companyDocRef.delete())
                                        .onErrorResume(t -> Mono.empty()) // ignore deletion failures during rollback
                                        .thenReturn(report);
                            });
                });
    }

    // Report DTOs
    public static class TestReport {
        private String suiteName;
        private String status;
        private String timestamp;
        private long totalDurationMs;
        private int testsRun;
        private int testsPassed;
        private int testsFailed;
        private List<TestResult> results = new ArrayList<>();

        public TestReport(String suiteName) {
            this.suiteName = suiteName;
            this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public void addResult(TestResult res) {
            this.results.add(res);
            this.testsRun++;
            if ("PASSED".equals(res.getStatus())) {
                this.testsPassed++;
            } else {
                this.testsFailed++;
            }
        }

        public String getSuiteName() {
            return suiteName;
        }

        public void setSuiteName(String suiteName) {
            this.suiteName = suiteName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }

        public long getTotalDurationMs() {
            return totalDurationMs;
        }

        public void setTotalDurationMs(long totalDurationMs) {
            this.totalDurationMs = totalDurationMs;
        }

        public int getTestsRun() {
            return testsRun;
        }

        public int getTestsPassed() {
            return testsPassed;
        }

        public int getTestsFailed() {
            return testsFailed;
        }

        public List<TestResult> getResults() {
            return results;
        }
    }

    public static class TestResult {
        private String testName;
        private String status;
        private long durationMs;
        private String details;

        public TestResult(String testName) {
            this.testName = testName;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(long durationMs) {
            this.durationMs = durationMs;
        }

        public String getDetails() {
            return details;
        }

        public void setDetails(String details) {
            this.details = details;
        }
    }
}
