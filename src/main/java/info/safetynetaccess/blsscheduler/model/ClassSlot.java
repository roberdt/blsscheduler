package info.safetynetaccess.blsscheduler.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "classslots")
public class ClassSlot {

    @DocumentId
    private String id;
    private String fullName;
    private String postNominalCredential;
    private String attendance; // Format: YYYY-MM-DD


    public ClassSlot() {
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPostNominalCredential() {
        return postNominalCredential;
    }

    public void setPostNominalCredential(String postNominalCredential) {
        this.postNominalCredential = postNominalCredential;
    }

    public String getAttendance() {
        return attendance;
    }

    public void setAttendance(String attendance) {
        this.attendance = attendance;
    }
}
