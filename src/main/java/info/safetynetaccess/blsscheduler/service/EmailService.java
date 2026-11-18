package info.safetynetaccess.blsscheduler.service;

import info.safetynetaccess.blsscheduler.dto.RegistrationRequest;
import reactor.core.publisher.Mono;

public interface EmailService {

    /**
     * Sends a welcome and registration confirmation email to a newly registered company administrator.
     *
     * @param request the registration request containing company and admin user details
     * @return Mono<Void> completing when the email has been handed off to the mail server
     */
    Mono<Void> sendRegistrationConfirmation(RegistrationRequest request);

    /**
     * Sends a registration confirmation email with individual fields.
     *
     * @param toEmail recipient email address
     * @param fullName full name of the user
     * @param companyName registered company name
     * @param purpose organization purpose
     * @return Mono<Void>
     */
    Mono<Void> sendRegistrationConfirmation(String toEmail, String fullName, String companyName, String purpose);

    /**
     * Sends an administrative alert notifying that a new company and user have registered.
     *
     * @param request the registration request
     * @param companyId the generated company ID
     * @param userId the generated administrator user ID
     * @return Mono<Void> completing when the alert has been handed off to the mail server
     */
    Mono<Void> sendAdminRegistrationAlert(RegistrationRequest request, String companyId, String userId);
}
