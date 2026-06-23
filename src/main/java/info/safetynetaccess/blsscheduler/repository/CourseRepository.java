package info.safetynetaccess.blsscheduler.repository;

import info.safetynetaccess.blsscheduler.model.Course;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends FirestoreReactiveRepository<Course> {
}
