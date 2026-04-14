package ifmo.se.lab1app.auth.security;

import ifmo.se.lab1app.auth.domain.AuthenticatedUser;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccountAuthenticationProvider implements AuthenticationProvider {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        UserAccount account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authentication failed"));

        if (!passwordMatches(password, account.getPassword())) {
            throw new BadCredentialsException("Authentication failed");
        }

        AuthenticatedUser user = AuthenticatedUser.fromRole(account.getUsername(), account.getRole());
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
