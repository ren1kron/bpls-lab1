package ifmo.se.lab1app.auth.application;

import ifmo.se.lab1app.auth.domain.UserAccount;
import ifmo.se.lab1app.auth.infra.UserAccountRepository;
import ifmo.se.lab1app.shared.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BaseModeratorBootstrap implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserSyncToOneCService userSyncToOneCService;

    @Value("${app.security.bootstrap.moderator.enabled:true}")
    private boolean enabled;

    @Value("${app.security.bootstrap.moderator.username:moderator}")
    private String username;

    @Value("${app.security.bootstrap.moderator.password:moderator}")
    private String password;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        userAccountRepository.findByUsername(username).ifPresentOrElse(
                this::normalizeModerator,
                this::createModerator
        );
    }

    private void createModerator() {
        UserAccount moderator = new UserAccount();
        moderator.setUsername(username);
        moderator.setPassword(passwordEncoder.encode(password));
        moderator.setRole(UserRole.COMPANY_MODERATOR);
        UserAccount saved = userAccountRepository.save(moderator);
        userSyncToOneCService.syncUserBestEffort(saved);
    }

    private void normalizeModerator(UserAccount moderator) {
        boolean changed = false;
        if (moderator.getRole() != UserRole.COMPANY_MODERATOR) {
            moderator.setRole(UserRole.COMPANY_MODERATOR);
            changed = true;
        }
        if (!hasPasswordEncoderPrefix(moderator.getPassword())) {
            String rawPassword = StringUtils.hasText(moderator.getPassword())
                    ? moderator.getPassword()
                    : password;
            moderator.setPassword(passwordEncoder.encode(rawPassword));
            changed = true;
        }

        if (changed) {
            UserAccount saved = userAccountRepository.save(moderator);
            userSyncToOneCService.syncUserBestEffort(saved);
        } else {
            userSyncToOneCService.syncUserBestEffort(moderator);
        }
    }

    private boolean hasPasswordEncoderPrefix(String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword) || encodedPassword.charAt(0) != '{') {
            return false;
        }
        int closingBraceIndex = encodedPassword.indexOf('}');
        return closingBraceIndex > 1;
    }
}
