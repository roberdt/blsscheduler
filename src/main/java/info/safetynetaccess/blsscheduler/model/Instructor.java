package info.safetynetaccess.blsscheduler.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "instructors")
public class Instructor {

    @DocumentId
    private String id;
    private String certificationid;
    private String firstname;
    private String lastname;
    private String postnominalcredential;
    private boolean active;

    public Instructor() {
    }

    public Instructor(String certificationid, String firstname, String lastname, String postnominalcredential, boolean active) {
        this.certificationid = certificationid;
        this.firstname = firstname;
        this.lastname = lastname;
        this.postnominalcredential = postnominalcredential;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCertificationid() {
        return certificationid;
    }

    public void setCertificationid(String certificationid) {
        this.certificationid = certificationid;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getPostnominalcredential() {
        return postnominalcredential;
    }

    public void setPostnominalcredential(String postnominalcredential) {
        this.postnominalcredential = postnominalcredential;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
