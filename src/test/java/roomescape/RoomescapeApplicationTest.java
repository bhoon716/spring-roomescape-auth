package roomescape;

import io.restassured.RestAssured;
import java.time.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import roomescape.common.web.LoginMember;
import roomescape.domain.reservation.repository.ReservationRepository;
import roomescape.domain.reservation.request.UserReservationCreateRequest;
import roomescape.domain.reservation.response.ReservationResponse;
import roomescape.domain.reservation.service.ReservationService;
import roomescape.domain.reservationtime.repository.ReservationTimeRepository;
import roomescape.domain.theme.repository.ThemeRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RoomescapeApplicationTest {

    private static final String TEST_USERNAME = "testuser1";
    private static final String TEST_PASSWORD = "password123";

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    @Test
    void contextLoads() {
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 인증이 필요한 예약을 조회하려고 하면 401 Unauthorized 예외 및 일관된 에러 응답을 반환한다")
    void getMyReservations_unauthorized() {
        RestAssured.given().log().all()
                .when().get("/reservations/me")
                .then().log().all()
                .statusCode(401)
                .body("message", equalTo("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("로그인하지 않은 사용자가 예약을 생성하려고 하면 401 Unauthorized 예외를 반환한다")
    void createReservation_unauthorized() {
        String body = """
                {
                    "themeId": 1,
                    "date": "2026-05-30",
                    "timeId": 1,
                    "storeId": 1
                }
                """;

        RestAssured.given().log().all()
                .contentType("application/json")
                .body(body)
                .when().post("/reservations")
                .then().log().all()
                .statusCode(401)
                .body("message", equalTo("로그인이 필요합니다."));
    }

    @Test
    @DisplayName("로그인에 성공하면 access token은 Authorization 헤더로 반환하고 refresh token은 쿠키로 설정한다")
    void login_success_returns_tokens() {
        signup(TEST_USERNAME, TEST_PASSWORD);

        RestAssured.given().log().all()
                .contentType("application/json")
                .body(loginBody(TEST_USERNAME, TEST_PASSWORD))
                .when().post("/auth/login")
                .then().log().all()
                .statusCode(200)
                .header("Authorization", startsWith("Bearer "))
                .cookie("refreshToken", notNullValue());
    }

    @Test
    @DisplayName("유효한 access token이 있으면 인증이 필요한 API에 접근할 수 있다")
    void getMyReservations_authorized_with_access_token() {
        signup(TEST_USERNAME, TEST_PASSWORD);
        String accessToken = loginAndExtractAccessToken(TEST_USERNAME, TEST_PASSWORD);

        RestAssured.given().log().all()
                .header("Authorization", "Bearer " + accessToken)
                .when().get("/reservations/me")
                .then().log().all()
                .statusCode(200);
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인을 시도하면 401 Unauthorized 실패 응답을 반환한다")
    void login_fail_with_wrong_password() {
        signup(TEST_USERNAME, TEST_PASSWORD);

        RestAssured.given().log().all()
                .contentType("application/json")
                .body(loginBody(TEST_USERNAME, "wrongpassword"))
                .when().post("/auth/login")
                .then().log().all()
                .statusCode(401)
                .body("message", equalTo("username 또는 password가 올바르지 않습니다."));
    }

    @Test
    @DisplayName("존재하지 않는 회원 이름으로 로그인을 시도하면 401 Unauthorized 실패 응답을 반환한다")
    void login_fail_with_non_existent_username() {
        String loginBody = """
                {
                    "username": "ghost",
                    "password": "password123"
                }
                """;
        RestAssured.given().log().all()
                .contentType("application/json")
                .body(loginBody)
                .when().post("/auth/login")
                .then().log().all()
                .statusCode(401)
                .body("message", equalTo("username 또는 password가 올바르지 않습니다."));
    }

    private void signup(String username, String password) {
        RestAssured.given().log().all()
                .contentType("application/json")
                .body(signupBody(username, password))
                .when().post("/auth/signup")
                .then().log().all()
                .statusCode(201);
    }

    private String loginAndExtractAccessToken(String username, String password) {
        return RestAssured.given().log().all()
                .contentType("application/json")
                .body(loginBody(username, password))
                .when().post("/auth/login")
                .then().log().all()
                .statusCode(200)
                .extract()
                .header("Authorization")
                .substring("Bearer ".length());
    }

    private String signupBody(String username, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s",
                    "email": "test1@gmail.com"
                }
                """.formatted(username, password);
    }

    private String loginBody(String username, String password) {
        return """
                {
                    "username": "%s",
                    "password": "%s"
                }
                """.formatted(username, password);
    }
}
