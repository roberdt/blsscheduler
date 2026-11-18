package info.safetynetaccess.blsscheduler;

import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import info.safetynetaccess.blsscheduler.service.EmailService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@Disabled("Manual live integration verification test - enable when testing live SMTP delivery")
@SpringBootTest(properties = {
        "app.admin.username=Admin",
        "app.admin.password=$2a$10$9SP2mGJJ4m26uPcqsSinyeUgZX.1GvNTj0JTyrcma9q6Azzqsz9sm",
        "app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B625064536766703373"
})
public class LiveEmailSenderTest {

    @Autowired
    private EmailService emailService;

    @Autowired
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Test
    void testLiveEmailSend() {
        if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl mailSenderImpl) {
            String username = mailSenderImpl.getUsername();
            String password = mailSenderImpl.getPassword();
            System.out.println("=== SMTP CONFIG INSPECTION ===");
            System.out.println("Username: '" + username + "'");
            if (password != null) {
                System.out.println("Password length: " + password.length());
                System.out.println("Password contains whitespace/spaces: " + password.matches(".*\\s+.*"));
                String strippedPassword = password.replaceAll("\\s+", "");
                System.out.println("Stripped password length: " + strippedPassword.length());
                mailSenderImpl.setPassword(strippedPassword);
                String preview = (strippedPassword.length() > 4) 
                        ? strippedPassword.substring(0, 2) + "****" + strippedPassword.substring(strippedPassword.length() - 2)
                        : "****";
                System.out.println("Password preview: " + preview);
            } else {
                System.out.println("Password is NULL!");
            }
            mailSenderImpl.getJavaMailProperties().put("mail.debug", "true");
            System.out.println("==============================");
        }

        RegistrationRequest testRequest = new RegistrationRequest(
                "Safety Net Access Testing",
                "Testing Gmail SMTP Live Delivery",
                "System Tester",
                "rerdt@safetynetaccess.info",
                "dummyPassword123!",
                "dummyPassword123!"
        );

        System.out.println("Starting live email dispatch test to rerdt@safetynetaccess.info...");
        emailService.sendRegistrationConfirmation(testRequest).block();
        System.out.println(">>> LIVE REGISTRATION EMAIL DISPATCH SUCCEEDED! Email sent to rerdt@safetynetaccess.info <<<");

        System.out.println("Starting live admin alert dispatch test to rerdt@safetynetaccess.info...");
        emailService.sendAdminRegistrationAlert(testRequest, "test-comp-123", "test-user-456").block();
        System.out.println(">>> LIVE ADMIN ALERT EMAIL DISPATCH SUCCEEDED! Email sent to rerdt@safetynetaccess.info <<<");
    }
}
