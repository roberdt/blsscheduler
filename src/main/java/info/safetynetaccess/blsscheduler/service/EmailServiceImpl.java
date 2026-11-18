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
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a z"));
        String safeFullName = escapeHtml(fullName != null ? fullName : "User");
        String safeCompanyName = escapeHtml(companyName != null ? companyName : "N/A");
        String safeEmail = escapeHtml(email != null ? email : "");
        String safePurpose = escapeHtml(purpose != null ? purpose : "Basic Life Support Scheduling");

        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Registration Confirmed</title>"
                + "<style>"
                + "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px; color: #333333; }"
                + "  .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.06); }"
                + "  .header { background: linear-gradient(135deg, #0d6efd 0%, #0a58ca 100%); color: #ffffff; padding: 32px 28px; text-align: center; }"
                + "  .header h1 { margin: 0; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; }"
                + "  .header p { margin: 8px 0 0; opacity: 0.9; font-size: 14px; }"
                + "  .content { padding: 32px 28px; }"
                + "  .greeting { font-size: 18px; font-weight: 600; margin-bottom: 16px; color: #1a1e21; }"
                + "  .intro { font-size: 15px; line-height: 1.6; color: #495057; margin-bottom: 24px; }"
                + "  .card { background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 24px; }"
                + "  .card-title { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #6c757d; margin-bottom: 12px; }"
                + "  .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; }"
                + "  .detail-row:last-child { border-bottom: none; }"
                + "  .detail-label { font-weight: 600; color: #495057; }"
                + "  .detail-value { color: #212529; text-align: right; word-break: break-word; }"
                + "  .footer { background-color: #f8f9fa; border-top: 1px solid #e9ecef; padding: 20px 28px; text-align: center; font-size: 12px; color: #6c757d; line-height: 1.5; }"
                + "  .footer a { color: #0d6efd; text-decoration: none; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class=\"container\">"
                + "    <div class=\"header\">"
                + "      <h1>Safety Net Access</h1>"
                + "      <p>BLS Training & Class Scheduler</p>"
                + "    </div>"
                + "    <div class=\"content\">"
                + "      <div class=\"greeting\">Hello " + safeFullName + ",</div>"
                + "      <div class=\"intro\">"
                + "        Welcome to <strong>Safety Net Access BLS Scheduler</strong>! Your organization profile and administrator account have been successfully created."
                + "      </div>"
                + "      <div class=\"card\">"
                + "        <div class=\"card-title\">Account & Organization Details</div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Organization:</span><span class=\"detail-value\">" + safeCompanyName + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Administrator:</span><span class=\"detail-value\">" + safeFullName + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Email:</span><span class=\"detail-value\">" + safeEmail + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Purpose:</span><span class=\"detail-value\">" + safePurpose + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Registered On:</span><span class=\"detail-value\">" + timestamp + "</span></div>"
                + "      </div>"
                + "      <p style=\"font-size: 14px; color: #495057; line-height: 1.6;\">"
                + "        You can now sign in with your credentials to manage courses, instructors, dates, and training class slots."
                + "      </p>"
                + "    </div>"
                + "    <div class=\"footer\">"
                + "      <p>This automated message was sent from <strong>" + escapeHtml(fromEmail) + "</strong>.</p>"
                + "      <p>Please do not reply directly to this email. If you did not request this registration, please contact your systems administrator.</p>"
                + "      <p>&copy; " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.</p>"
                + "    </div>"
                + "  </div>"
                + "</body>"
                + "</html>";
    }

    private String buildRegistrationPlainText(String fullName, String companyName, String email, String purpose) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a z"));
        return "Hello " + (fullName != null ? fullName : "User") + ",\n\n"
                + "Welcome to Safety Net Access BLS Scheduler!\n"
                + "Your organization profile and administrator account have been successfully created.\n\n"
                + "--------------------------------------------------\n"
                + "Account & Organization Details:\n"
                + "- Organization: " + (companyName != null ? companyName : "N/A") + "\n"
                + "- Administrator: " + (fullName != null ? fullName : "N/A") + "\n"
                + "- Email: " + (email != null ? email : "") + "\n"
                + "- Purpose: " + (purpose != null ? purpose : "Basic Life Support Scheduling") + "\n"
                + "- Registered On: " + timestamp + "\n"
                + "--------------------------------------------------\n\n"
                + "You can now sign in with your credentials to manage courses, instructors, and class slots.\n\n"
                + "---\n"
                + "This automated message was sent from " + fromEmail + ".\n"
                + "Please do not reply directly to this email.\n"
                + "© " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.\n";
    }

    private String buildAdminAlertHtml(RegistrationRequest request, String companyId, String userId) {
        String timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy h:mm a z"));
        String safeCompanyName = escapeHtml(request.getCompanyName() != null ? request.getCompanyName() : "N/A");
        String safeCompanyId = escapeHtml(companyId != null ? companyId : "N/A");
        String safeFullName = escapeHtml(request.getFullname() != null ? request.getFullname() : "N/A");
        String safeEmail = escapeHtml(request.getEmailAddress() != null ? request.getEmailAddress() : "N/A");
        String safeUserId = escapeHtml(userId != null ? userId : "N/A");
        String safePurpose = escapeHtml(request.getPurpose() != null ? request.getPurpose() : "N/A");

        return "<!DOCTYPE html>"
                + "<html lang=\"en\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>New Registration Alert</title>"
                + "<style>"
                + "  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; background-color: #f4f6f9; margin: 0; padding: 24px; color: #333333; }"
                + "  .container { max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 16px rgba(0,0,0,0.06); }"
                + "  .header { background: linear-gradient(135deg, #1e293b 0%, #0f172a 100%); color: #ffffff; padding: 32px 28px; text-align: center; border-bottom: 4px solid #0d6efd; }"
                + "  .badge { display: inline-block; background-color: rgba(13, 110, 253, 0.2); color: #60a5fa; border: 1px solid rgba(96, 165, 250, 0.4); border-radius: 20px; padding: 4px 12px; font-size: 11px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.8px; margin-bottom: 12px; }"
                + "  .header h1 { margin: 0; font-size: 22px; font-weight: 700; letter-spacing: -0.5px; }"
                + "  .header p { margin: 8px 0 0; opacity: 0.85; font-size: 13px; }"
                + "  .content { padding: 32px 28px; }"
                + "  .intro { font-size: 15px; line-height: 1.6; color: #495057; margin-bottom: 24px; }"
                + "  .card { background-color: #f8f9fa; border: 1px solid #e9ecef; border-radius: 8px; padding: 20px; margin-bottom: 24px; }"
                + "  .card-title { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.5px; color: #0f172a; margin-bottom: 14px; padding-bottom: 8px; border-bottom: 2px solid #e2e8f0; }"
                + "  .detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #dee2e6; font-size: 14px; }"
                + "  .detail-row:last-child { border-bottom: none; }"
                + "  .detail-label { font-weight: 600; color: #495057; }"
                + "  .detail-value { color: #0f172a; text-align: right; word-break: break-word; font-family: monospace, monospace; font-size: 13px; }"
                + "  .footer { background-color: #f8f9fa; border-top: 1px solid #e9ecef; padding: 20px 28px; text-align: center; font-size: 12px; color: #6c757d; line-height: 1.5; }"
                + "</style>"
                + "</head>"
                + "<body>"
                + "  <div class=\"container\">"
                + "    <div class=\"header\">"
                + "      <div class=\"badge\">System Administrator Alert</div>"
                + "      <h1>New Company Registered</h1>"
                + "      <p>Safety Net Access BLS Training & Class Scheduler</p>"
                + "    </div>"
                + "    <div class=\"content\">"
                + "      <div class=\"intro\">"
                + "        A new organization profile and administrator user account have registered on the platform."
                + "      </div>"
                + "      <div class=\"card\">"
                + "        <div class=\"card-title\">Registration Details</div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Organization Name:</span><span class=\"detail-value\" style=\"font-family: inherit; font-weight: 600;\">" + safeCompanyName + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Company ID:</span><span class=\"detail-value\">" + safeCompanyId + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Administrator Name:</span><span class=\"detail-value\" style=\"font-family: inherit;\">" + safeFullName + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Administrator Email:</span><span class=\"detail-value\" style=\"font-family: inherit;\">" + safeEmail + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">User ID:</span><span class=\"detail-value\">" + safeUserId + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Organization Purpose:</span><span class=\"detail-value\" style=\"font-family: inherit;\">" + safePurpose + "</span></div>"
                + "        <div class=\"detail-row\"><span class=\"detail-label\">Registered On:</span><span class=\"detail-value\" style=\"font-family: inherit;\">" + timestamp + "</span></div>"
                + "      </div>"
                + "    </div>"
                + "    <div class=\"footer\">"
                + "      <p>This automated administrative notification was sent from <strong>" + escapeHtml(fromEmail) + "</strong>.</p>"
                + "      <p>&copy; " + ZonedDateTime.now().getYear() + " Safety Net Access. All rights reserved.</p>"
                + "    </div>"
                + "  </div>"
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
