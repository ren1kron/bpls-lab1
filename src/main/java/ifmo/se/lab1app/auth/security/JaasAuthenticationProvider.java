package ifmo.se.lab1app.auth.security;

import ifmo.se.lab1app.auth.domain.AuthenticatedUser;
import ifmo.se.lab1app.auth.jaas.JaasPrivilegePrincipal;
import ifmo.se.lab1app.auth.jaas.JaasRolePrincipal;
import ifmo.se.lab1app.auth.jaas.JaasUserPrincipal;
import ifmo.se.lab1app.auth.jaas.UsernamePasswordCallbackHandler;
import ifmo.se.lab1app.auth.jaas.DatabaseUserJaasConfiguration;
import ifmo.se.lab1app.shared.domain.UserRole;
import java.util.Set;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JaasAuthenticationProvider implements AuthenticationProvider {

    private final DatabaseUserJaasConfiguration jaasConfiguration;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = String.valueOf(authentication.getPrincipal());
        String password = String.valueOf(authentication.getCredentials());

        try {
            LoginContext loginContext = new LoginContext(
                    DatabaseUserJaasConfiguration.LOGIN_CONTEXT_NAME,
                    null,
                    new UsernamePasswordCallbackHandler(username, password),
                    jaasConfiguration
            );
            loginContext.login();
            AuthenticatedUser user = toAuthenticatedUser(loginContext.getSubject());
            return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
        } catch (LoginException exception) {
            throw new BadCredentialsException("Authentication failed", exception);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private AuthenticatedUser toAuthenticatedUser(Subject subject) {
        String username = subject.getPrincipals(JaasUserPrincipal.class).stream()
                .findFirst()
                .map(JaasUserPrincipal::getName)
                .orElseThrow(() -> new BadCredentialsException("JAAS subject does not contain username"));

        UserRole role = subject.getPrincipals(JaasRolePrincipal.class).stream()
                .findFirst()
                .map(JaasRolePrincipal::getName)
                .map(UserRole::valueOf)
                .orElseThrow(() -> new BadCredentialsException("JAAS subject does not contain role"));

        Set<String> privileges = subject.getPrincipals(JaasPrivilegePrincipal.class).stream()
                .map(JaasPrivilegePrincipal::getName)
                .collect(Collectors.toUnmodifiableSet());

        return new AuthenticatedUser(username, role, privileges);
    }
}
