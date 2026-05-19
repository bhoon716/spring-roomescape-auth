package roomescape.domain.user.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import roomescape.domain.user.service.AuthService;

@WebMvcTest
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Mock
    AuthService authService;

    @InjectMocks
    AuthController authController;

    @Test
    @DisplayName("로그인 테스트")
    void loginTest() {
        // given
        String body = """
                {
                    "username": "user1",
                    "password": "password1"
                }
                """;

//        mockMvc.perform(
//                post("/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(body)
//                )
//                .andExpect(status().isOk())
//                .andExpect(request().sessionAttribute())

        // when

        // then
    }
}