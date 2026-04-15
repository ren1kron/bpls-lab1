package ifmo.se.lab1app.auth.api;

import ifmo.se.lab1app.auth.api.dto.LoginRequest;
import ifmo.se.lab1app.auth.api.dto.LoginResponse;
import ifmo.se.lab1app.auth.api.dto.RegisterRequest;
import ifmo.se.lab1app.auth.api.dto.UserResponse;
import ifmo.se.lab1app.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(
        name = "Auth",
        description = "Авторизация в систему рекламной компании Aviasales"
)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Войти по имени пользователя и паролю",
            description = "Аутентифицирует пользователя через JAAS по данным из БД и возвращает JWT. "
                    + "Базовый модератор: moderator/moderator (COMPANY_MODERATOR)")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    @Operation(summary = "Зарегистрировать клиента",
            description = "Создает пользователя роли CLIENT, сохраняет его в БД, аутентифицирует через JAAS и возвращает JWT")
    public LoginResponse register(@RequestBody @Valid RegisterRequest request) {
        return authService.register(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Текущий пользователь",
            description = "Возвращает информацию об аутентифицированном пользователе по JWT")
    public UserResponse me(Authentication authentication) {
        return authService.currentUser(authentication);
    }
}
