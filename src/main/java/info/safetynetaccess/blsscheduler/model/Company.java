package info.safetynetaccess.blsscheduler.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "companies")
public class Company {

    @DocumentId
    private String id;
    private String name;
    private String purpose;
    private String approval = "No";

    public Company() {
    }

    public Company(String name, String purpose) {
        this.name = name;
        this.purpose = purpose;
    }

    public Company(String name, String purpose, String approval) {
        this.name = name;
        this.purpose = purpose;
        this.approval = approval;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getApproval() {
        return approval;
    }

    public void setApproval(String approval) {
        this.approval = approval;
    }
}
