package info.safetynetaccess.blsscheduler.filter;

import info.safetynetaccess.blsscheduler.service.UserDetailsService;
import info.safetynetaccess.blsscheduler.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    private final boolean DEBUG = true;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if  ( DEBUG ) {
            System.out.println("[JWT Filter] ---> Incoming request: " + request.getMethod() + " " + request.getRequestURI());
            System.out.println("[JWT Filter] Authorization Header: " + (authorizationHeader != null ? "'" + authorizationHeader + "'" : "NULL"));
        }

        String username = null;
        String jwt = null;

        if (authorizationHeader != null && (authorizationHeader.trim().startsWith("Bearer ") || authorizationHeader.trim().startsWith("bearer "))) {
            jwt = authorizationHeader.trim().substring(7).trim();
            System.out.println("[JWT Filter] Extracted JWT: '" + jwt + "'");
            try {
                username = jwtUtil.extractUsername(jwt);
                if  ( DEBUG ) {
                    System.out.println("[JWT Filter] Extracted Username from JWT: '" + username + "'");
                }
            } catch (Exception e) {
                if  ( DEBUG ) {
                    System.out.println("[JWT Filter] ERROR parsing JWT: " + e.getMessage());
                }
                logger.warn("Failed to parse JWT token: " + e.getMessage());
            }
        } else if (authorizationHeader != null) {
            System.out.println("[JWT Filter] WARNING: Authorization header does not start with 'Bearer ' or 'bearer '");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                if  ( DEBUG ) {
                    System.out.println("[JWT Filter] Loading UserDetails for username: '" + username + "'");
                }
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                boolean isValid = jwtUtil.validateToken(jwt, userDetails);
                if  ( DEBUG ) {
                    System.out.println("[JWT Filter] Token validation result: " + isValid);
                }

                if (isValid) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                    System.out.println("[JWT Filter] SUCCESSFULLY Authenticated user '" + username + "', security context set.");
                } else {
                    System.out.println("[JWT Filter] WARNING: Token is invalid or expired for user '" + username + "'");
                    logger.warn(String.format("JWT Token validation failed. Token username: '%s', UserDetails username: '%s'",
                            username, userDetails.getUsername()));
                }
            } catch (Exception e) {
                System.out.println("[JWT Filter] ERROR during authentication process: " + e.getMessage());
                logger.warn("Failed to authenticate user via JWT: " + e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
