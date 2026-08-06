package info.safetynetaccess.blsscheduler.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "classslots")
public class ClassSlot {

    @DocumentId
    private String id;
    private String ahaid;
    private String firstname;
    private String lastname;
    private String email;
    private String postnominalcredential;
    private String attendance; // Show or No Show
    private String passfail; // Pass, Fail, No Grade
    private String score;
    private String notes;


    public ClassSlot() {
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAhaid() {
        return ahaid;
    }

    public void setAhaid(String ahaid) {
        this.ahaid = ahaid;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPostnominalcredential() {
        return postnominalcredential;
    }

    public void setPostnominalcredential(String postnominalcredential) {
        this.postnominalcredential = postnominalcredential;
    }

    public String getAttendance() {
        return attendance;
    }

    public void setAttendance(String attendance) {
        this.attendance = attendance;
    }

    public String getPassfail() {
        return passfail;
    }

    public void setPassfail(String passfail) {
        this.passfail = passfail;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
