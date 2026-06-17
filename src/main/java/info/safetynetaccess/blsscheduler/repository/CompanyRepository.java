package info.safetynetaccess.blsscheduler.repository;

import info.safetynetaccess.blsscheduler.model.Company;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRepository extends FirestoreReactiveRepository<Company> {
}
