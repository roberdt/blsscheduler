package info.safetynetaccess.blsscheduler.service;

import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import jakarta.mail.Address;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSender);
        ReflectionTestUtils.setField(emailService, "fromEmail", "safetynet@safetynetaccess.org");
        ReflectionTestUtils.setField(emailService, "senderName", "Safety Net Access BLS Scheduler");
        ReflectionTestUtils.setField(emailService, "adminNotificationEmail", "rerdt@safetynetaccess.info");
    }

    @Test
    void testSendRegistrationConfirmation_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        RegistrationRequest request = new RegistrationRequest(
                "Acme Health",
                "BLS Certification",
                "Jane Doe",
                "jane.doe@example.com",
                "123-456-7890",
                "password123",
                "password123"
        );

        StepVerifier.create(emailService.sendRegistrationConfirmation(request))
                .verifyComplete();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);

        try {
            Address[] recipients = sentMessage.getAllRecipients();
            assertNotNull(recipients);
            assertEquals(1, recipients.length);
            assertEquals("jane.doe@example.com", ((InternetAddress) recipients[0]).getAddress());

            Address[] from = sentMessage.getFrom();
            assertNotNull(from);
            assertEquals(1, from.length);
            assertEquals("safetynet@safetynetaccess.org", ((InternetAddress) from[0]).getAddress());

            assertEquals("Welcome to Safety Net Access - Account Registration Confirmed", sentMessage.getSubject());
        } catch (Exception e) {
            fail("Exception accessing MIME message properties: " + e.getMessage());
        }
    }

    @Test
    void testSendRegistrationConfirmation_FailurePropagatesError() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP server connection refused"))
                .when(mailSender).send(any(MimeMessage.class));

        RegistrationRequest request = new RegistrationRequest(
                "Acme Health",
                "BLS Certification",
                "Jane Doe",
                "jane.doe@example.com",
                "123-456-7890",
                "password123",
                "password123"
        );

        StepVerifier.create(emailService.sendRegistrationConfirmation(request))
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && throwable.getMessage().contains("Email sending failed"))
                .verify();
    }

    @Test
    void testSendAdminRegistrationAlert_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        RegistrationRequest request = new RegistrationRequest(
                "Acme Health",
                "BLS Certification",
                "Jane Doe",
                "jane.doe@example.com",
                "123-456-7890",
                "password123",
                "password123"
        );

        StepVerifier.create(emailService.sendAdminRegistrationAlert(request, "company-123", "user-456"))
                .verifyComplete();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);

        try {
            Address[] recipients = sentMessage.getAllRecipients();
            assertNotNull(recipients);
            assertEquals(1, recipients.length);
            assertEquals("rerdt@safetynetaccess.info", ((InternetAddress) recipients[0]).getAddress());

            Address[] from = sentMessage.getFrom();
            assertNotNull(from);
            assertEquals(1, from.length);
            assertEquals("safetynet@safetynetaccess.org", ((InternetAddress) from[0]).getAddress());

            assertEquals("New Registration Alert: Acme Health", sentMessage.getSubject());
        } catch (Exception e) {
            fail("Exception accessing MIME message properties: " + e.getMessage());
        }
    }

    @Test
    void testSendAdminRegistrationAlert_SkippedWhenEmailBlank() {
        ReflectionTestUtils.setField(emailService, "adminNotificationEmail", "");

        RegistrationRequest request = new RegistrationRequest(
                "Acme Health",
                "BLS Certification",
                "Jane Doe",
                "jane.doe@example.com",
                "123-456-7890",
                "password123",
                "password123"
        );

        StepVerifier.create(emailService.sendAdminRegistrationAlert(request, "company-123", "user-456"))
                .verifyComplete();

        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}
