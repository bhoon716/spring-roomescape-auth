package roomescape;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RoomescapeApplicationTest {

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
                    "timeId": 1
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
    @DisplayName("잘못된 비밀번호로 로그인을 시도하면 401 Unauthorized 실패 응답을 반환한다")
    void login_fail_with_wrong_password() {
        // 1. 회원가입 진행
        String signupBody = """
                {
                    "username": "testuser1",
                    "password": "password123",
                    "email": "test1@gmail.com"
                }
                """;
        RestAssured.given().log().all()
                .contentType("application/json")
                .body(signupBody)
                .when().post("/auth/signup")
                .then().log().all()
                .statusCode(201);

        // 2. 잘못된 비밀번호로 로그인 시도
        String loginBody = """
                {
                    "username": "testuser1",
                    "password": "wrongpassword"
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
}
