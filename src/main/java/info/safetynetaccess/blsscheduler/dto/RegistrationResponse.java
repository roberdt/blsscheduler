package info.safetynetaccess.blsscheduler.dto;

public class RegistrationResponse {

    private boolean success;
    private String message;
    private String companyId;
    private String userId;

    public RegistrationResponse() {
    }

    public RegistrationResponse(boolean success, String message, String companyId, String userId) {
        this.success = success;
        this.message = message;
        this.companyId = companyId;
        this.userId = userId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
