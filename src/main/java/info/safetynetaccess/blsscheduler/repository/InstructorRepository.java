package info.safetynetaccess.blsscheduler.repository;

import info.safetynetaccess.blsscheduler.model.Instructor;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorRepository extends FirestoreReactiveRepository<Instructor> {
}
