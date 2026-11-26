package dev.learninghub.shared;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Current-user lookup for feature modules (architecture section 2). The principal name is the account id.
 * Until sign-in exists (S5), dev mode falls back to a seeded learner configured only in the %dev profile.
 */
@ApplicationScoped
public class CurrentUser {

    private final Optional<UUID> devUserId;

    public CurrentUser(@ConfigProperty(name = "learninghub.dev.current-user-id") Optional<UUID> devUserId) {
        this.devUserId = devUserId;
    }

    public UUID id(SecurityContext securityContext) {
        Principal principal = securityContext.getUserPrincipal();
        if (principal != null) {
            try {
                return UUID.fromString(principal.getName());
            } catch (IllegalArgumentException e) {
                throw unauthorized();
            }
        }
        return devUserId.orElseThrow(CurrentUser::unauthorized);
    }

    private static NotAuthorizedException unauthorized() {
        return new NotAuthorizedException(Response.status(Response.Status.UNAUTHORIZED).build());
    }
}
