package info.safetynetaccess.blsscheduler.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.DocumentSnapshot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserDetailsService implements org.springframework.security.core.userdetails.UserDetailsService {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Autowired
    private Firestore firestore;

    private final PasswordEncoder passwordEncoder;

    public UserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("[UserDetailsService] Attempting to load user: '" + username + "'");
        System.out.println("[UserDetailsService] Configured adminUsername: '" + adminUsername + "', adminPassword: '" + adminPassword + "'");
        if (adminUsername.equals(username)) {
            // We return the password as is, without re-encoding it.
            // The AuthenticationManager will handle the encoding and comparison.
            return new User(adminUsername, adminPassword, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        } else if (username != null && username.contains(":")) {
            String[] parts = username.split(":", 2);
            String realUsername = parts[0];
            String companyId = parts[1];

            try {
                CollectionReference usersRef = firestore.collection("companies")
                        .document(companyId)
                        .collection("users");

                // Query by username
                Query query = usersRef.whereEqualTo("username", realUsername);
                QuerySnapshot querySnapshot = query.get().get();

                // Fallback: Query by email
                if (querySnapshot.isEmpty()) {
                    query = usersRef.whereEqualTo("email", realUsername);
                    querySnapshot = query.get().get();
                }

                if (querySnapshot.isEmpty()) {
                    throw new UsernameNotFoundException("User not found with username/email: " + realUsername + " in company: " + companyId);
                }

                QueryDocumentSnapshot userDoc = querySnapshot.getDocuments().get(0);
                String userId = userDoc.getId();

                DocumentSnapshot passwordDoc = firestore.collection("companies")
                        .document(companyId)
                        .collection("users")
                        .document(userId)
                        .collection("passwords")
                        .document("credentials")
                        .get().get();

                if (!passwordDoc.exists()) {
                    throw new UsernameNotFoundException("Credentials not found for user: " + realUsername);
                }

                String hashedPassword = passwordDoc.getString("hashedPassword");
                if (hashedPassword == null) {
                    throw new UsernameNotFoundException("Hashed password is null for user: " + realUsername);
                }

                // Return user details with composite username so JWT validation matches
                return new User(username, hashedPassword, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            } catch (Exception e) {
                System.err.println("Error loading company user in UserDetailsService: " + e.getMessage());
                throw new UsernameNotFoundException("Failed to load user " + realUsername + " in company " + companyId, e);
            }
        } else {
            // Here you would add your logic to find regular users from the Firestore database
            // For now, we'll keep the hardcoded "user" for demonstration
            if ("user".equals(username)) {
                // Return pre-hashed BCrypt of "password"
                return new User("user", "$2a$10$gRstA5V5YzS..E9WkFEPp.nO729mGvO3uNfQ88gX9/0hM9Z9SfevG", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            }
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
    }
}