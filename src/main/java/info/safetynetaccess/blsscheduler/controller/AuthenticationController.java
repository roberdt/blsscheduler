package info.safetynetaccess.blsscheduler.controller;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import info.safetynetaccess.blsscheduler.dto.AuthenticationRequest;
import info.safetynetaccess.blsscheduler.dto.AuthenticationResponse;
import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import info.safetynetaccess.blsscheduler.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Firestore firestore;

    @Value("${app.admin.username}")
    private String adminUsername;

    @PostMapping("/api/authenticate")
    public ResponseEntity<?> createAuthenticationToken(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        String username = authenticationRequest.getUsername();
        String password = authenticationRequest.getPassword();
        String companyId = authenticationRequest.getCompanyId();

        System.out.println("[AuthenticationController] Attempting login. Request username: '" + username + "', adminUsername: '" + adminUsername + "'");
        String compositeUsername;
        if (adminUsername.equals(username)) {
            compositeUsername = username;
        } else {
            if (companyId == null || companyId.trim().isEmpty()) {
                throw new BadCredentialsException("Company ID is required for non-Admin users");
            }
            compositeUsername = username + ":" + companyId.trim();
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(compositeUsername, password)
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(compositeUsername);
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }

    @PostMapping("/api/authenticate-by-company-name")
    public ResponseEntity<?> createAuthenticationTokenByCompanyName(@RequestBody AuthenticationRequest authenticationRequest) throws Exception {
        String username = authenticationRequest.getUsername();
        String password = authenticationRequest.getPassword();
        String companyName = authenticationRequest.getCompanyName();

        String compositeUsername;
        if (adminUsername.equals(username)) {
            compositeUsername = username;
        } else {
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new BadCredentialsException("Company name is required for non-Admin users");
            }

            try {
                CollectionReference companiesRef = firestore.collection("companies");
                Query query = companiesRef.whereEqualTo("name", companyName.trim());
                QuerySnapshot querySnapshot = query.get().get();

                if (querySnapshot.isEmpty()) {
                    throw new BadCredentialsException("Company not found with name: " + companyName);
                }

                String companyId = querySnapshot.getDocuments().get(0).getId();
                authenticationRequest.setCompanyId(companyId);
                compositeUsername = username + ":" + companyId;
            } catch (BadCredentialsException e) {
                throw e;
            } catch (Exception e) {
                System.err.println("Error looking up company by name in AuthenticationController: " + e.getMessage());
                throw new Exception("Error during company lookup", e);
            }
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(compositeUsername, password)
            );
        } catch (BadCredentialsException e) {
            throw new Exception("Incorrect username or password", e);
        }

        final UserDetails userDetails = userDetailsService.loadUserByUsername(compositeUsername);
        final String jwt = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(new AuthenticationResponse(jwt));
    }
}
