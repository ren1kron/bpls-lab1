package ifmo.se.lab1app.config;

import ifmo.se.lab1app.shared.domain.User;
import ifmo.se.lab1app.shared.domain.UserRole;
import ifmo.se.lab1app.auth.infra.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        createUserIfNotExists("client", "client", UserRole.CLIENT);
        createUserIfNotExists("moderator", "moderator", UserRole.COMPANY_MODERATOR);
    }

    private void createUserIfNotExists(String username, String rawPassword, UserRole role) {
        if (!userRepository.existsByUsername(username)) {
            String encoded = passwordEncoder.encode(rawPassword);
            userRepository.save(new User(username, encoded, role));
            log.info("Bootstrapped user '{}' with role {}", username, role);
        }
    }
}
