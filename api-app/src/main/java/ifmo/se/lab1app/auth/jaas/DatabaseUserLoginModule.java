package ifmo.se.lab1app.auth.jaas;

import ifmo.se.lab1app.auth.domain.Privilege;
import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import org.springframework.security.crypto.password.PasswordEncoder;

public class DatabaseUserLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;
    private UserAccount authenticatedAccount;
    private final Set<Principal> committedPrincipals = new HashSet<>();

    @Override
    public void initialize(
            Subject subject,
            CallbackHandler callbackHandler,
            Map<String, ?> sharedState,
            Map<String, ?> options
    ) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = options;
    }

    @Override
    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("Callback handler is required");
        }

        NameCallback usernameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);

        try {
            callbackHandler.handle(new Callback[]{usernameCallback, passwordCallback});
        } catch (IOException | UnsupportedCallbackException exception) {
            throw new LoginException("Unable to read JAAS callbacks: " + exception.getMessage());
        }

        String username = usernameCallback.getName();
        char[] passwordChars = passwordCallback.getPassword();
        String password = passwordChars == null ? "" : new String(passwordChars);
        passwordCallback.clearPassword();

        UserAccount account = userAccountRepository().findByUsername(username)
                .orElseThrow(() -> new FailedLoginException("Unknown user: " + username));

        if (!passwordMatches(password, account.getPassword())) {
            throw new FailedLoginException("Invalid password");
        }

        authenticatedAccount = account;
        return true;
    }

    @Override
    public boolean commit() {
        if (authenticatedAccount == null) {
            return false;
        }

        committedPrincipals.add(new JaasUserPrincipal(authenticatedAccount.getUsername()));
        committedPrincipals.add(new JaasRolePrincipal(authenticatedAccount.getRole().name()));
        authenticatedAccount.getRole().privileges().stream()
                .map(Privilege::authority)
                .map(JaasPrivilegePrincipal::new)
                .forEach(committedPrincipals::add);

        subject.getPrincipals().addAll(committedPrincipals);
        return true;
    }

    @Override
    public boolean abort() {
        clearState();
        return true;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().removeAll(committedPrincipals);
        clearState();
        return true;
    }

    private UserAccountRepository userAccountRepository() throws LoginException {
        Object value = options.get(DatabaseUserJaasConfiguration.USER_ACCOUNT_REPOSITORY_OPTION);
        if (value instanceof UserAccountRepository userAccountRepository) {
            return userAccountRepository;
        }
        throw new LoginException("JAAS user account repository is not configured");
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) throws LoginException {
        Object value = options.get(DatabaseUserJaasConfiguration.PASSWORD_ENCODER_OPTION);
        if (!(value instanceof PasswordEncoder passwordEncoder)) {
            throw new LoginException("JAAS password encoder is not configured");
        }
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void clearState() {
        authenticatedAccount = null;
        committedPrincipals.clear();
    }
}
