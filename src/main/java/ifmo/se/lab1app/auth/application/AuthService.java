package ifmo.se.lab1app.auth.application;

import ifmo.se.lab1app.auth.api.dto.LoginRequest;
import ifmo.se.lab1app.auth.api.dto.UserResponse;
import ifmo.se.lab1app.exception.NotFoundException;
import ifmo.se.lab1app.shared.domain.User;
import ifmo.se.lab1app.auth.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.security.Principal;

/**
 * Authentication service.ww
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                request.username(), request.password());
        Authentication authenticated = authenticationManager.authenticate(authToken);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);

        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", context);

        User user = findByUsername(request.username());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public UserResponse currentUser(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Not authenticated");
        }
        User user = findByUsername(principal.getName());
        return new UserResponse(user.getId(), user.getUsername(), user.getRole());
    }

    public User resolveCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return findByUsername(auth.getName());
    }

    private User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
    }
}
