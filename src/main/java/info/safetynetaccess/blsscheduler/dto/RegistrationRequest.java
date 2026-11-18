package info.safetynetaccess.blsscheduler.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String companyName;

    @NotBlank(message = "Purpose is required")
    @Size(max = 1000, message = "Purpose must not exceed 1000 characters")
    private String purpose;

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullname;

    @NotBlank(message = "Email address is required")
    @Email(message = "Email address should be valid")
    private String emailAddress;

    @NotBlank(message = "Phone number is required")
    @Size(min = 7, max = 25, message = "Phone number must be between 7 and 25 characters")
    @JsonAlias({"phoneNumber", "phone"})
    private String phonenumber;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;

    @NotBlank(message = "Verify password is required")
    private String verifyPassword;

    public RegistrationRequest() {
    }

    public RegistrationRequest(String companyName, String purpose, String fullname, String emailAddress, String password, String verifyPassword) {
        this(companyName, purpose, fullname, emailAddress, null, password, verifyPassword);
    }

    public RegistrationRequest(String companyName, String purpose, String fullname, String emailAddress, String phonenumber, String password, String verifyPassword) {
        this.companyName = companyName;
        this.purpose = purpose;
        this.fullname = fullname;
        this.emailAddress = emailAddress;
        this.phonenumber = phonenumber;
        this.password = password;
        this.verifyPassword = verifyPassword;
    }

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatching() {
        if (password == null) {
            return verifyPassword == null;
        }
        return password.equals(verifyPassword);
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getVerifyPassword() {
        return verifyPassword;
    }

    public void setVerifyPassword(String verifyPassword) {
        this.verifyPassword = verifyPassword;
    }
}
