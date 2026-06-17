package info.safetynetaccess.blsscheduler.repository;

import info.safetynetaccess.blsscheduler.model.Time;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

// Time is a sub-collection of Date, so the third generic parameter is the parent ID (Date ID).
@Repository
public interface TimeRepository extends FirestoreReactiveRepository<Time> {
}
