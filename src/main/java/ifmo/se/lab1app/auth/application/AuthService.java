package ifmo.se.lab1app.auth.application;

import ifmo.se.lab1app.auth.api.dto.LoginRequest;
import ifmo.se.lab1app.auth.api.dto.LoginResponse;
import ifmo.se.lab1app.auth.api.dto.UserResponse;
import ifmo.se.lab1app.auth.domain.AuthenticatedUser;
import ifmo.se.lab1app.auth.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public LoginResponse login(LoginRequest request) {
        Authentication authToken = new UsernamePasswordAuthenticationToken(
                request.username(), request.password());
        Authentication authenticated = authenticationManager.authenticate(authToken);
        AuthenticatedUser user = (AuthenticatedUser) authenticated.getPrincipal();

        return new LoginResponse(
                jwtTokenService.issueToken(user),
                "Bearer",
                toUserResponse(user)
        );
    }

    public UserResponse currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(AuthenticatedUser user) {
        return new UserResponse(user.username(), user.role(), user.privileges());
    }
}
