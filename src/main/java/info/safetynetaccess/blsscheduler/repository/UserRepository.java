package info.safetynetaccess.blsscheduler.repository;

import info.safetynetaccess.blsscheduler.model.User;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

// User is a sub-collection of Company, so the third generic parameter is the parent ID (Company ID).
@Repository
public interface UserRepository extends FirestoreReactiveRepository<User> {
}
