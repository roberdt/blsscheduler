package info.safetynetaccess.blsscheduler.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "dates")
public class Date {

    @DocumentId
    private String id;
    private String dateValue; // e.g., "2023-10-27"

    public Date() {
    }

    public Date(String dateValue) {
        this.dateValue = dateValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDateValue() {
        return dateValue;
    }

    public void setDateValue(String dateValue) {
        this.dateValue = dateValue;
    }
}
