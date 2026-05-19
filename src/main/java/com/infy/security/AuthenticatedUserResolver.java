package com.infy.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.infy.entity.User;
import com.infy.exception.WellnessTrackerException;
import com.infy.repository.UserRepository;

/**
 * Centralised JWT identity resolver.
 *
 *
 * Role-level guards (@PreAuthorize) are handled at the service interface level.
 * This resolver only provides the identity — it never checks roles.
 *
 * Usage in services:
 *   User caller = authenticatedUserResolver.resolveCurrentUser();
 *   Integer callerId = authenticatedUserResolver.resolveCurrentUserId();
 */
@Component
public class AuthenticatedUserResolver {

    @Autowired
    private UserRepository userRepository;

    /**
     * Returns the full User entity for the currently authenticated caller.
     * Derived from the JWT subject (email) stored in the SecurityContext.
     *
     * Throws Service.UNAUTHORIZED if no valid authentication is present.
     * Throws Service.USER_NOT_FOUND if the email in the JWT no longer maps to a user.
     */
    public User resolveCurrentUser() throws WellnessTrackerException {
        String email = resolveCurrentEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new WellnessTrackerException("Service.USER_NOT_FOUND"));
    }

    /**
     * Returns the userId for the currently authenticated caller.
     * Convenience wrapper around resolveCurrentUser().
     */
    public Integer resolveCurrentUserId() throws WellnessTrackerException {
        return resolveCurrentUser().getUserId();
    }

    /**
     * Returns the email (JWT subject) for the currently authenticated caller.
     * Use this when only the email is needed and a DB call should be avoided.
     */
    public String resolveCurrentEmail() throws WellnessTrackerException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || "anonymousUser".equals(auth.getPrincipal())) {
            throw new WellnessTrackerException("Service.UNAUTHORIZED");
        }
        return auth.getName(); // JWT subject — always the user's email
    }
}
