package roomescape.domain.auth.controller;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import roomescape.domain.auth.token.RefreshTokenCookieProvider;
import roomescape.domain.auth.request.LoginRequest;
import roomescape.domain.auth.request.SignupRequest;
import roomescape.domain.auth.response.SignupResponse;
import roomescape.domain.auth.service.AuthService;
import roomescape.domain.auth.token.JwtTokenPair;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    public AuthController(
            AuthService authService,
            RefreshTokenCookieProvider refreshTokenCookieProvider
    ) {
        this.authService = authService;
        this.refreshTokenCookieProvider = refreshTokenCookieProvider;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.created(URI.create("/users/" + response.id()))
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody LoginRequest request) {
        JwtTokenPair tokenPair = authService.login(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, tokenPair.getAuthorizationHeader())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokenPair.refreshToken()).toString())
                .build();
    }

    @PostMapping("/reissue")
    public ResponseEntity<Void> reissue(
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        JwtTokenPair tokenPair = authService.reissue(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, tokenPair.getAuthorizationHeader())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.create(tokenPair.refreshToken()).toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        authService.logout(refreshToken);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieProvider.expire().toString())
                .build();
    }
}
