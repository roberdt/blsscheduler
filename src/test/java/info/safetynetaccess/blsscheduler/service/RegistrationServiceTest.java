package info.safetynetaccess.blsscheduler.service;

import com.google.api.core.SettableApiFuture;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import info.safetynetaccess.blsscheduler.dto.RegistrationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RegistrationServiceTest {

    @Mock
    private Firestore firestore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private CollectionReference companiesCollection;

    @Mock
    private DocumentReference companyDocRef;

    @Mock
    private CollectionReference usersCollection;

    @Mock
    private DocumentReference userDocRef;

    @Mock
    private CollectionReference passwordsCollection;

    @Mock
    private DocumentReference passwordDocRef;

    @InjectMocks
    private RegistrationService registrationService;

    private RegistrationRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = new RegistrationRequest(
                "HealthCorp",
                "CPR Training",
                "John Doe",
                "john.doe@example.com",
                "123-456-7890",
                "secret123",
                "secret123"
        );
    }

    @Test
    void testRegister_SuccessWithEmailSent() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashedSecret");

        when(firestore.collection("companies")).thenReturn(companiesCollection);
        when(companiesCollection.document(anyString())).thenReturn(companyDocRef);

        when(companyDocRef.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(anyString())).thenReturn(userDocRef);

        when(userDocRef.collection("passwords")).thenReturn(passwordsCollection);
        when(passwordsCollection.document("credentials")).thenReturn(passwordDocRef);

        SettableApiFuture<WriteResult> future = SettableApiFuture.create();
        future.set(mock(WriteResult.class));

        doReturn(future).when(companyDocRef).set(any(Object.class));
        doReturn(future).when(userDocRef).set(any(Object.class));
        doReturn(future).when(passwordDocRef).set(any(Object.class));

        when(emailService.sendRegistrationConfirmation(any(RegistrationRequest.class)))
                .thenReturn(Mono.empty());
        when(emailService.sendAdminRegistrationAlert(any(RegistrationRequest.class), anyString(), anyString()))
                .thenReturn(Mono.empty());

        StepVerifier.create(registrationService.register(sampleRequest))
                .assertNext(response -> {
                    assertTrue(response.isSuccess(), "Response was not success: " + response.getMessage());
                    assertEquals("Company and administrator user registered successfully", response.getMessage());
                    assertNotNull(response.getCompanyId());
                    assertNotNull(response.getUserId());
                })
                .verifyComplete();

        verify(emailService, times(1)).sendRegistrationConfirmation(sampleRequest);
        verify(emailService, times(1)).sendAdminRegistrationAlert(eq(sampleRequest), anyString(), anyString());
    }

    @Test
    void testRegister_SuccessEvenIfEmailServiceFails() {
        when(passwordEncoder.encode("secret123")).thenReturn("hashedSecret");

        when(firestore.collection("companies")).thenReturn(companiesCollection);
        when(companiesCollection.document(anyString())).thenReturn(companyDocRef);

        when(companyDocRef.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document(anyString())).thenReturn(userDocRef);

        when(userDocRef.collection("passwords")).thenReturn(passwordsCollection);
        when(passwordsCollection.document("credentials")).thenReturn(passwordDocRef);

        SettableApiFuture<WriteResult> future = SettableApiFuture.create();
        future.set(mock(WriteResult.class));

        doReturn(future).when(companyDocRef).set(any(Object.class));
        doReturn(future).when(userDocRef).set(any(Object.class));
        doReturn(future).when(passwordDocRef).set(any(Object.class));

        when(emailService.sendRegistrationConfirmation(any(RegistrationRequest.class)))
                .thenReturn(Mono.error(new RuntimeException("SMTP connection timeout")));
        when(emailService.sendAdminRegistrationAlert(any(RegistrationRequest.class), anyString(), anyString()))
                .thenReturn(Mono.error(new RuntimeException("Admin alert timeout")));

        StepVerifier.create(registrationService.register(sampleRequest))
                .assertNext(response -> {
                    assertTrue(response.isSuccess(), "Response was not success: " + response.getMessage());
                    assertEquals("Company and administrator user registered successfully", response.getMessage());
                    assertNotNull(response.getCompanyId());
                    assertNotNull(response.getUserId());
                })
                .verifyComplete();

        verify(emailService, times(1)).sendRegistrationConfirmation(sampleRequest);
        verify(emailService, times(1)).sendAdminRegistrationAlert(eq(sampleRequest), anyString(), anyString());
    }
}
