package info.safetynetaccess.blsscheduler.dto;

public class AuthenticationRequest {

    private String username;
    private String password;
    private String companyId;
    private String companyName;

    public AuthenticationRequest() {
    }

    public AuthenticationRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public AuthenticationRequest(String username, String password, String companyId) {
        this.username = username;
        this.password = password;
        this.companyId = companyId;
    }

    public AuthenticationRequest(String username, String password, String companyId, String companyName) {
        this.username = username;
        this.password = password;
        this.companyId = companyId;
        this.companyName = companyName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}
