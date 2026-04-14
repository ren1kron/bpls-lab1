package ifmo.se.lab1app.auth.application;

import ifmo.se.lab1app.auth.domain.AuthenticatedUser;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.shared.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public AuthenticatedUser requireAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new AccessDeniedException("Authentication required");
        }
        return user;
    }

    public UserAccount requireCurrentUserAccount() {
        String username = requireAuthenticatedUser().username();
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("Authenticated user is not available"));
    }

    public boolean hasRole(UserRole role) {
        return requireAuthenticatedUser().role() == role;
    }
}
