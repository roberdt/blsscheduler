package info.safetynetaccess.blsscheduler.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

@Document(collectionName = "times")
public class Time {

    @DocumentId
    private String id;
    private String startTimeValue; // e.g., "09:00 AM"
    private String endTimeValue; // e.g., "09:00 AM"
    private int numberOfSSlots;

    public Time() {

    }

    public String getStartTimeValue() {
        return startTimeValue;
    }

    public void setStartTimeValue(String startTimeValue) {
        this.startTimeValue = startTimeValue;
    }

    public String getEndTimeValue() {
        return endTimeValue;
    }

    public void setEndTimeValue(String endTimeValue) {
        this.endTimeValue = endTimeValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    public int getNumberOfSSlots() {
        return numberOfSSlots;
    }

    public void setNumberOfSSlots(int numberOfSSlots) {
        this.numberOfSSlots = numberOfSSlots;
    }
}
