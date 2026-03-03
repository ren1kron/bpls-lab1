package ifmo.se.lab1app.auth.api;

import ifmo.se.lab1app.auth.api.dto.LoginRequest;
import ifmo.se.lab1app.auth.api.dto.UserResponse;
import ifmo.se.lab1app.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(
        name = "Auth",
        description = "Авторизация в систему рекламной компании Aviasales"
)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Войти по имени пользователя и паролю",
            description = "Аутентифицирует пользователя и создаёт HTTP-сессию. "
                    + "Предзаданные пользователи: client/client (CLIENT), moderator/moderator (COMPANY_MODERATOR)")
    public UserResponse login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        return authService.login(request, httpRequest);
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь",
            description = "Возвращает информацию об аутентифицированном пользователе (по сессии или HTTP Basic)")
    public UserResponse me(Principal principal) {
        return authService.currentUser(principal);
    }

    @PostMapping("/logout")
    @Operation(summary = "Выйти из системы", description = "Инвалидирует текущую HTTP-сессию")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }
}
