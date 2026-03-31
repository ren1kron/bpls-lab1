package ifmo.se.lab1app.auth.jaas;

import ifmo.se.lab1app.auth.domain.Privilege;
import ifmo.se.lab1app.auth.domain.XmlUserAccount;
import ifmo.se.lab1app.shared.domain.UserRole;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
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
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class XmlUserLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;
    private XmlUserAccount authenticatedAccount;
    private final Set<Principal> committedPrincipals = new HashSet<>();
    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

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

        XmlUserAccount account = loadAccount(username)
                .orElseThrow(() -> new FailedLoginException("Unknown user: " + username));

        if (!passwordEncoder.matches(password, account.password())) {
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

        committedPrincipals.add(new JaasUserPrincipal(authenticatedAccount.username()));
        committedPrincipals.add(new JaasRolePrincipal(authenticatedAccount.role().name()));
        authenticatedAccount.role().privileges().stream()
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

    private Optional<XmlUserAccount> loadAccount(String username) throws LoginException {
        try (InputStream inputStream = openUsersXml()) {
            DocumentBuilderFactory factory = secureDocumentBuilderFactory();
            Document document = factory.newDocumentBuilder().parse(inputStream);
            NodeList users = document.getElementsByTagName("user");
            for (int i = 0; i < users.getLength(); i++) {
                org.w3c.dom.Node node = users.item(i);
                if (!(node instanceof org.w3c.dom.Element element)) {
                    continue;
                }
                if (!username.equals(element.getAttribute("username"))) {
                    continue;
                }

                return Optional.of(new XmlUserAccount(
                        element.getAttribute("username"),
                        element.getAttribute("password"),
                        UserRole.valueOf(element.getAttribute("role"))
                ));
            }
            return Optional.empty();
        } catch (Exception exception) {
            throw new LoginException("Unable to load users XML: " + exception.getMessage());
        }
    }

    private InputStream openUsersXml() throws IOException {
        String location = String.valueOf(options.get(XmlUserJaasConfiguration.USERS_XML_LOCATION_OPTION));
        if (location.startsWith("classpath:")) {
            String resourcePath = location.substring("classpath:".length());
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
            if (stream == null) {
                throw new IOException("Resource not found: " + location);
            }
            return stream;
        }
        return Files.newInputStream(Path.of(location));
    }

    private DocumentBuilderFactory secureDocumentBuilderFactory() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory;
    }

    private void clearState() {
        authenticatedAccount = null;
        committedPrincipals.clear();
    }
}
