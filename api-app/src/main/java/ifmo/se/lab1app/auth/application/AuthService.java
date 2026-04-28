package ifmo.se.lab1app.auth.application;

import ifmo.se.lab1app.auth.api.dto.LoginRequest;
import ifmo.se.lab1app.auth.api.dto.LoginResponse;
import ifmo.se.lab1app.auth.api.dto.RegisterRequest;
import ifmo.se.lab1app.auth.api.dto.UserResponse;
import ifmo.se.lab1app.auth.domain.AuthenticatedUser;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.auth.security.JwtTokenService;
import ifmo.se.lab1app.exception.AlreadyExistsException;
import ifmo.se.lab1app.shared.application.TransactionExecutor;
import ifmo.se.lab1app.shared.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionExecutor transactions;

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

    public LoginResponse register(RegisterRequest request) {
        transactions.write(() -> {
            if (userAccountRepository.existsByUsername(request.username())) {
                throw new AlreadyExistsException("Пользователь с username=" + request.username() + " уже существует");
            }

            UserAccount userAccount = new UserAccount();
            userAccount.setUsername(request.username());
            userAccount.setPassword(passwordEncoder.encode(request.password()));
            userAccount.setRole(UserRole.CLIENT);

            userAccountRepository.save(userAccount);
        });
        return login(new LoginRequest(request.username(), request.password()));
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
