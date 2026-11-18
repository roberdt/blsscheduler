package info.safetynetaccess.blsscheduler.service;

import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:safetynet@safetynetaccess.org}}")
    private String fromEmail;

    @Value("${app.mail.sender-name:Safety Net Access BLS Scheduler}")
    private String senderName;

    @Value("${app.mail.admin-notification-email:rerdt@safetynetaccess.info}")
    private String adminNotificationEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public Mono<Void> sendRegistrationConfirmation(RegistrationRequest request) {
        return sendRegistrationConfirmation(
                request.getEmailAddress(),
                request.getFullname(),
                request.getCompanyName(),
                request.getPurpose()
        );
    }

    @Override
    public Mono<Void> sendRegistrationConfirmation(String toEmail, String fullName, String companyName, String purpose) {
        return Mono.fromRunnable(() -> {
            try {
                sendConfirmationEmailSync(toEmail, fullName, companyName, purpose);
            } catch (Exception e) {
                log.error("Failed to send registration confirmation email to {}: {}", toEmail, e.getMessage(), e);
                throw new RuntimeException("Email sending failed for " + toEmail, e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    @Override
    public Mono<Void> sendAdminRegistrationAlert(RegistrationRequest request, String companyId, String userId) {
        if (adminNotificationEmail == null || adminNotificationEmail.isBlank()) {
            log.debug("Admin notification email address is not configured; skipping admin registration alert.");
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
            try {
                sendAdminAlertEmailSync(request, companyId, userId);
            } catch (Exception e) {
                log.error("Failed to send admin registration alert to {}: {}", adminNotificationEmail, e.getMessage(), e);
                throw new RuntimeException("Admin alert email sending failed", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    private void sendAdminAlertEmailSync(RegistrationRequest request, String companyId, String userId)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail, senderName);
        helper.setTo(adminNotificationEmail);
        String subject = "New Registration Alert: " + (request.getCompanyName() != null ? request.getCompanyName() : "New Organization");
        helper.setSubject(subject);

        String htmlContent = buildAdminAlertHtml(request, companyId, userId);
        String textContent = buildAdminAlertPlainText(request, companyId, userId);

        helper.setText(textContent, htmlContent);

        mailSender.send(message);
        log.info("Admin registration alert email successfully dispatched to {} for company '{}' (ID: {})",
                adminNotificationEmail, request.getCompanyName(), companyId);
    }

    private void sendConfirmationEmailSync(String toEmail, String fullName, String companyName, String purpose)
            throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(
                message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromEmail, senderName);
        helper.setTo(toEmail);
        helper.setSubject("Welcome to Safety Net Access - Account Registration Confirmed");

        String htmlContent = buildRegistrationHtml(fullName, companyName, toEmail, purpose);
        String textContent = buildRegistrationPlainText(fullName, companyName, toEmail, purpose);

        helper.setText(textContent, htmlContent);

        mailSender.send(message);
        log.info("Registration confirmation email successfully dispatched to {} from {}", toEmail, fromEmail);
    }

    private String buildRegistrationHtml(String fullName, String companyName, String email, String purpose) {
        String safeFullName = escapeHtml(fullName != null ? fullName : "Valued Customer");
        String safeCompanyName = escapeHtml(companyName != null ? companyName : "your organization");
        String safeFromEmail = escapeHtml(fromEmail);

        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Welcome to Safety Net Access</title>"
                + "</head>"
                + "<body bgcolor=\"#f4f6f9\" style=\"margin: 0; padding: 24px; background-color: #f4f6f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #333333;\">"
                + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f4f6f9\" style=\"background-color: #f4f6f9;\">"
                + "    <tr>"
                + "      <td align=\"center\" style=\"padding: 12px;\">"
                + "        <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#ffffff\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border: 1px solid #dce3ec; border-radius: 12px; overflow: hidden;\">"
                + "          <!-- Colored Banner Header -->"
                + "          <tr>"
                + "            <td align=\"center\" bgcolor=\"#0d6efd\" style=\"background-color: #0d6efd; padding: 36px 28px; text-align: center; border-bottom: 4px solid #0a58ca;\">"
                + "              <h1 style=\"margin: 0; font-size: 26px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;\">Welcome to Safety Net Access!</h1>"
                + "              <p style=\"margin: 8px 0 0; font-size: 15px; color: #e0ecff;\">BLS Training &amp; Class Scheduler</p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Customer Welcome Body -->"
                + "          <tr>"
                + "            <td bgcolor=\"#ffffff\" style=\"padding: 32px 28px; background-color: #ffffff; text-align: left;\">"
                + "              <p style=\"margin: 0 0 16px; font-size: 18px; font-weight: 600; color: #1a1e21;\">Hello " + safeFullName + ",</p>"
                + "              <p style=\"margin: 0 0 18px; font-size: 15px; line-height: 1.6; color: #495057;\">"
                + "                Thank you for registering <strong>" + safeCompanyName + "</strong> with <strong>Safety Net Access BLS Scheduler</strong>! We are excited to partner with you for your Basic Life Support training and scheduling needs."
                + "              </p>"
                + "              <!-- Highlighted Next Steps Callout Box -->"
                + "              <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#eef5ff\" style=\"background-color: #eef5ff; border-left: 4px solid #0d6efd; border-radius: 6px; margin: 24px 0;\">"
                + "                <tr>"
                + "                  <td style=\"padding: 20px;\">"
                + "                    <p style=\"margin: 0 0 8px; font-size: 14px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #0a58ca;\">What Happens Next?</p>"
                + "                    <p style=\"margin: 0; font-size: 15px; line-height: 1.6; color: #1e293b;\">"
                + "                      We have received your registration information. A member of our team is reviewing your request and <strong>will contact you shortly</strong> to assist with onboarding and answer any questions you may have."
                + "                    </p>"
                + "                  </td>"
                + "                </tr>"
                + "              </table>"
                + "              <p style=\"margin: 0 0 16px; font-size: 15px; line-height: 1.6; color: #495057;\">"
                + "                In the meantime, you can sign in to explore the platform and begin preparing your courses, instructors, and training schedules."
                + "              </p>"
                + "              <p style=\"margin: 24px 0 0; font-size: 15px; line-height: 1.6; color: #1a1e21;\">"
                + "                Warm regards,<br>"
                + "                <strong style=\"color: #0d6efd;\">The Safety Net Access Team</strong>"
                + "              </p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Footer -->"
                + "          <tr>"
                + "            <td align=\"center\" bgcolor=\"#f8f9fa\" style=\"background-color: #f8f9fa; border-top: 1px solid #e9ecef; padding: 20px 28px; text-align: center; font-size: 12px; color: #6c757d; line-height: 1.5;\">"
                + "              <p style=\"margin: 4px 0;\">This message was sent from <strong>" + safeFromEmail + "</strong>.</p>"
                + "              <p style=\"margin: 4px 0;\">&copy; " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.</p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String buildRegistrationPlainText(String fullName, String companyName, String email, String purpose) {
        return "Hello " + (fullName != null ? fullName : "Valued Customer") + ",\n\n"
                + "Welcome to Safety Net Access BLS Scheduler!\n\n"
                + "Thank you for registering " + (companyName != null ? companyName : "your organization")
                + " with Safety Net Access. We are excited to partner with you for your Basic Life Support training and scheduling needs.\n\n"
                + "WHAT HAPPENS NEXT?\n"
                + "We have received your registration information. A member of our team is reviewing your request and will contact you shortly to assist with onboarding and answer any questions you may have.\n\n"
                + "In the meantime, you can sign in to explore the platform and begin preparing your courses, instructors, and training schedules.\n\n"
                + "Warm regards,\n"
                + "The Safety Net Access Team\n\n"
                + "---\n"
                + "This automated message was sent from " + fromEmail + ".\n"
                + "© " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.\n";
    }

    private String buildAdminAlertHtml(RegistrationRequest request, String companyId, String userId) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a z"));
        String safeCompanyName = escapeHtml(request.getCompanyName() != null ? request.getCompanyName() : "N/A");
        String safeCompanyId = escapeHtml(companyId != null ? companyId : "N/A");
        String safeFullName = escapeHtml(request.getFullname() != null ? request.getFullname() : "N/A");
        String safeEmail = escapeHtml(request.getEmailAddress() != null ? request.getEmailAddress() : "N/A");
        String safePhone = escapeHtml(request.getPhonenumber() != null && !request.getPhonenumber().isBlank() ? request.getPhonenumber() : "N/A");
        String safeUserId = escapeHtml(userId != null ? userId : "N/A");
        String safePurpose = escapeHtml(request.getPurpose() != null ? request.getPurpose() : "N/A");

        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>New Registration Alert</title>"
                + "</head>"
                + "<body bgcolor=\"#f4f6f9\" style=\"margin: 0; padding: 24px; background-color: #f4f6f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #333333;\">"
                + "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f4f6f9\" style=\"background-color: #f4f6f9;\">"
                + "    <tr>"
                + "      <td align=\"center\" style=\"padding: 12px;\">"
                + "        <table width=\"600\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#ffffff\" style=\"max-width: 600px; width: 100%; background-color: #ffffff; border: 1px solid #dce3ec; border-radius: 12px; overflow: hidden;\">"
                + "          <!-- Admin Colored Header -->"
                + "          <tr>"
                + "            <td align=\"center\" bgcolor=\"#1e293b\" style=\"background-color: #1e293b; padding: 32px 28px; text-align: center; border-bottom: 4px solid #0d6efd;\">"
                + "              <div style=\"display: inline-block; background-color: #0d6efd; color: #ffffff; border-radius: 20px; padding: 4px 12px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 12px;\">System Administrator Alert</div>"
                + "              <h1 style=\"margin: 0; font-size: 22px; font-weight: 700; color: #ffffff; letter-spacing: -0.5px;\">New Company Registered</h1>"
                + "              <p style=\"margin: 8px 0 0; font-size: 13px; color: #cbd5e1;\">Safety Net Access BLS Training &amp; Class Scheduler</p>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Admin Alert Body with Full Details Table -->"
                + "          <tr>"
                + "            <td bgcolor=\"#ffffff\" style=\"padding: 32px 28px; background-color: #ffffff; text-align: left;\">"
                + "              <p style=\"margin: 0 0 24px; font-size: 15px; line-height: 1.6; color: #495057;\">"
                + "                A new organization profile and administrator user account have registered on the platform. Please review the details below and follow up with the customer."
                + "              </p>"
                + "              <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" bgcolor=\"#f8f9fa\" style=\"background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px;\">"
                + "                <tr>"
                + "                  <td colspan=\"2\" style=\"font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #0f172a; padding-bottom: 10px; border-bottom: 2px solid #cbd5e1;\">Customer &amp; System Registration Details</td>"
                + "                </tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">Organization Name:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 700; color: #0f172a; text-align: right;\">" + safeCompanyName + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">Company ID:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 13px; font-family: monospace; color: #0f172a; text-align: right;\">" + safeCompanyId + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">Administrator Name:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; color: #0f172a; text-align: right;\">" + safeFullName + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">Administrator Email:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; color: #0d6efd; text-align: right;\">" + safeEmail + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">Administrator Phone:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; color: #0f172a; text-align: right;\">" + safePhone + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">User ID:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 13px; font-family: monospace; color: #0f172a; text-align: right;\">" + safeUserId + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; font-weight: 600; color: #495057;\">Organization Purpose:</td><td align=\"right\" style=\"padding: 10px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; color: #0f172a; text-align: right;\">" + safePurpose + "</td></tr>"
                + "                <tr><td style=\"padding: 10px 0; font-size: 14px; font-weight: 600; color: #495057;\">Registered On:</td><td align=\"right\" style=\"padding: 10px 0; font-size: 14px; color: #0f172a; text-align: right;\">" + timestamp + "</td></tr>"
                + "              </table>"
                + "            </td>"
                + "          </tr>"
                + "          <!-- Footer -->"
                + "          <tr>"
                + "            <td align=\"center\" bgcolor=\"#f8f9fa\" style=\"background-color: #f8f9fa; border-top: 1px solid #e9ecef; padding: 20px 28px; text-align: center; font-size: 12px; color: #6c757d; line-height: 1.5;\">"
                + "              <p style=\"margin: 4px 0;\">This automated administrative notification was sent from <strong>" + escapeHtml(fromEmail) + "</strong>.</p>"
                + "              <p style=\"margin: 4px 0;\">&copy; " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.</p>"
                + "            </td>"
                + "          </tr>"
                + "        </table>"
                + "      </td>"
                + "    </tr>"
                + "  </table>"
                + "</body>"
                + "</html>";
    }

    private String buildAdminAlertPlainText(RegistrationRequest request, String companyId, String userId) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a z"));
        return "=== Safety Net Access BLS Scheduler: New Registration Alert ===\n\n"
                + "A new organization and administrator user account have registered on the platform.\n\n"
                + "Registration Details:\n"
                + "- Organization Name: " + (request.getCompanyName() != null ? request.getCompanyName() : "N/A") + "\n"
                + "- Company ID: " + (companyId != null ? companyId : "N/A") + "\n"
                + "- Administrator Name: " + (request.getFullname() != null ? request.getFullname() : "N/A") + "\n"
                + "- Administrator Email: " + (request.getEmailAddress() != null ? request.getEmailAddress() : "N/A") + "\n"
                + "- Administrator Phone: " + (request.getPhonenumber() != null && !request.getPhonenumber().isBlank() ? request.getPhonenumber() : "N/A") + "\n"
                + "- User ID: " + (userId != null ? userId : "N/A") + "\n"
                + "- Organization Purpose: " + (request.getPurpose() != null ? request.getPurpose() : "N/A") + "\n"
                + "- Registered On: " + timestamp + "\n\n"
                + "---\n"
                + "This automated administrative message was sent from " + fromEmail + ".\n"
                + "© " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.\n";
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
