package info.safetynetaccess.blsscheduler;

import info.safetynetaccess.blsscheduler.util.JwtUtil;
import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
		"app.admin.username=Admin",
		"app.admin.password=$2a$10$9SP2mGJJ4m26uPcqsSinyeUgZX.1GvNTj0JTyrcma9q6Azzqsz9sm",
		"app.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B625064536766703373"
})
class BlsschedulerApplicationTests {

	@Autowired
	private JwtUtil jwtUtil;

	@Autowired
	private UserDetailsService userDetailsService;

	@Test
	void testJwtFlow() {
		// 1. Load UserDetails for Admin
		UserDetails adminDetails = userDetailsService.loadUserByUsername("Admin");
		assertNotNull(adminDetails, "Admin UserDetails should not be null");
		assertEquals("Admin", adminDetails.getUsername(), "Username should match");

		// 2. Generate Token
		String token = jwtUtil.generateToken(adminDetails);
		assertNotNull(token, "Generated token should not be null");
		assertFalse(token.isEmpty(), "Generated token should not be empty");

		// 3. Extract Username from Token
		String extractedUsername = jwtUtil.extractUsername(token);
		assertEquals("Admin", extractedUsername, "Extracted username should be Admin");

		// 4. Validate Token
		boolean isValid = jwtUtil.validateToken(token, adminDetails);
		assertTrue(isValid, "Token should be valid for the admin user");
	}
}