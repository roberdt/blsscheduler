package info.safetynetaccess.blsscheduler.service;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.common.util.concurrent.MoreExecutors;
import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import info.safetynetaccess.blsscheduler.dto.RegistrationResponse;
import info.safetynetaccess.blsscheduler.model.Company;
import info.safetynetaccess.blsscheduler.model.Password;
import info.safetynetaccess.blsscheduler.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    @Autowired
    private Firestore firestore;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

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

    public Mono<RegistrationResponse> register(RegistrationRequest request) {
        // Generate IDs
        String companyId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();

        // Create domain models
        Company company = new Company(request.getCompanyName(), request.getPurpose());
        company.setId(companyId);

        User user = new User(request.getFullname(), request.getEmailAddress());
        user.setId(userId);

        String hashedPassword = passwordEncoder.encode(request.getPassword());
        Password password = new Password(hashedPassword);
        password.setId("credentials");

        // Define references for hierarchical storage
        // 1. companies/{companyId}
        DocumentReference companyRef = firestore.collection("companies")
                .document(companyId);

        // 2. companies/{companyId}/users/{userId}
        DocumentReference userRef = companyRef.collection("users")
                .document(userId);

        // 3. companies/{companyId}/users/{userId}/passwords/credentials
        DocumentReference passwordRef = userRef.collection("passwords")
                .document("credentials");

        // Write sequentially and reactive-ly, then dispatch confirmation and admin alert emails
        return fromApiFuture(companyRef.set(company))
                .then(fromApiFuture(userRef.set(user)))
                .then(fromApiFuture(passwordRef.set(password)))
                .then(Mono.when(
                        emailService.sendRegistrationConfirmation(request)
                                .onErrorResume(e -> {
                                    log.error("Failed to send registration confirmation email to {}: {}", request.getEmailAddress(), e.getMessage());
                                    return Mono.empty();
                                }),
                        emailService.sendAdminRegistrationAlert(request, companyId, userId)
                                .onErrorResume(e -> {
                                    log.error("Failed to send admin registration alert for company {}: {}", companyId, e.getMessage());
                                    return Mono.empty();
                                })
                ))
                .thenReturn(new RegistrationResponse(
                        true,
                        "Company and administrator user registered successfully",
                        companyId,
                        userId
                ))
                .onErrorResume(t -> Mono.just(new RegistrationResponse(
                        false,
                        "Registration failed: " + t.getMessage(),
                        null,
                        null
                )));
    }
}
