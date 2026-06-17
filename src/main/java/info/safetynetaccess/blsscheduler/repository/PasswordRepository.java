package info.safetynetaccess.blsscheduler.repository;

import info.safetynetaccess.blsscheduler.model.Password;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

// Password is a sub-collection of User, so the third generic parameter is the parent ID (User ID).
@Repository
public interface PasswordRepository extends FirestoreReactiveRepository<Password> {
}
