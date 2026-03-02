package com.mas.gov.bt.mas.primary.config;


import com.mas.gov.bt.mas.primary.exception.UnauthorizedOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Utility component to extract user information from the JWT token
 * in the current security context.
 */
@Component
public class UserContext {

    /**
     * Get the current authenticated user's ID from the JWT token.
     * Looks for 'userId' claim.
     *
     * @return the user ID
     * @throws UnauthorizedOperationException if user is not authenticated
     */
    public Long getCurrentUserId() {
        Jwt jwt = getJwt();

        // Get 'userId' claim from token
        Object userId = jwt.getClaim("userId");
        if (userId != null) {
            return convertToLong(userId);
        }

        throw new UnauthorizedOperationException("User ID not found in token");
    }

    /**
     * Get the current authenticated user's username from the JWT token.
     * The username is stored in the 'sub' (subject) claim.
     *
     * @return the username
     * @throws UnauthorizedOperationException if user is not authenticated
     */
    public String getCurrentUsername() {
        Jwt jwt = getJwt();
        return jwt.getSubject();
    }

    /**
     * Get the current authenticated user's email from the JWT token.
     *
     * @return the email, or null if not present
     */
    public String getCurrentUserEmail() {
        Jwt jwt = getJwt();
        return jwt.getClaim("email");
    }

    /**
     * Get the current authenticated user's type from the JWT token.
     *
     * @return the user type, or null if not present
     */
    public String getCurrentUserType() {
        Jwt jwt = getJwt();
        return jwt.getClaim("userType");
    }

    /**
     * Check if the current user is an Agency user.
     *
     * @return true if userType is "Agency", false otherwise
     */
    public boolean isAgencyUser() {
        String userType = getCurrentUserType();
        return "Agency".equalsIgnoreCase(userType);
    }

    /**
     * Get a specific claim from the JWT token.
     *
     * @param claimName the name of the claim
     * @param <T> the type of the claim value
     * @return the claim value, or null if not present
     */
    public <T> T getClaim(String claimName) {
        Jwt jwt = getJwt();
        return jwt.getClaim(claimName);
    }

    /**
     * Get the raw JWT token.
     *
     * @return the JWT
     * @throws UnauthorizedOperationException if user is not authenticated
     */
    public Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedOperationException("User not authenticated");
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt;
        }

        throw new UnauthorizedOperationException("Invalid authentication type");
    }

    private Long convertToLong(Object value) {
        if (value instanceof Long l) {
            return l;
        }
        if (value instanceof Integer i) {
            return i.longValue();
        }
        if (value instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                throw new UnauthorizedOperationException("Invalid user ID format in token");
            }
        }
        throw new UnauthorizedOperationException("Cannot convert user ID from token");
    }
}
