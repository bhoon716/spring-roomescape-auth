package roomescape.common.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.reservation.repository.ReservationRepository;
import roomescape.domain.store.entity.Manager;
import roomescape.domain.store.repository.ManagerRepository;
import roomescape.domain.user.entity.User;
import roomescape.domain.user.entity.UserRole;
import roomescape.domain.user.repository.UserRepository;

class ReservationAccessAspectTest {

    private UserRepository userRepository;
    private ManagerRepository managerRepository;
    private ReservationRepository reservationRepository;
    private ReservationAccessAspect aspect;

    private ProceedingJoinPoint joinPoint;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        managerRepository = mock(ManagerRepository.class);
        reservationRepository = mock(ReservationRepository.class);
        aspect = new ReservationAccessAspect(userRepository, managerRepository, reservationRepository);

        joinPoint = mock(ProceedingJoinPoint.class);
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void setLoginMember(Long userId, String username) {
        request.setAttribute("loginMember", new LoginMember(userId, username));
    }

    @Test
    @DisplayName("로그인 정보가 없으면 UNAUTHORIZED 예외를 던진다")
    void checkReservationAccess_fail_unauthorized() {
        // given
        // No loginMember attribute set in request

        // when & then
        assertThatThrownBy(() -> aspect.checkReservationAccess(joinPoint))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("어드민 권한일 경우 예약 접근 인가를 무조건 통과한다")
    void checkReservationAccess_admin_success() throws Throwable {
        // given
        setLoginMember(1L, "admin");
        User admin = User.of(1L, "admin", "password", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        // when
        aspect.checkReservationAccess(joinPoint);

        // then
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("어드민 권한일 경우 매장 접근 인가를 무조건 통과한다")
    void checkStoreAccess_admin_success() throws Throwable {
        // given
        setLoginMember(1L, "admin");
        User admin = User.of(1L, "admin", "password", UserRole.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        // when
        aspect.checkStoreAccess(joinPoint);

        // then
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("매니저가 관리하는 매장의 예약일 경우 예약 접근 인가를 정상 통과한다")
    void checkReservationAccess_manager_success() throws Throwable {
        // given
        setLoginMember(2L, "manager1");
        User managerUser = User.of(2L, "manager1", "pass", UserRole.MANAGER);
        Manager managerDomain = new Manager(10L, 2L, List.of(100L, 101L));

        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(managerRepository.findByUserId(2L)).thenReturn(Optional.of(managerDomain));

        // PathVariable 세팅: reservationId = 500
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("reservationId", "500");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

        // 예약이 관리하는 매장 목록(100, 101)에 존재함
        when(reservationRepository.existsByIdAndStoreIdIn(500L, List.of(100L, 101L))).thenReturn(true);

        // when
        aspect.checkReservationAccess(joinPoint);

        // then
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("매니저가 관리하지 않는 매장의 예약일 경우 NOT_FOUND(404 은폐 전략) 예외를 던진다")
    void checkReservationAccess_manager_fail_otherStoreReservation() {
        // given
        setLoginMember(2L, "manager1");
        User managerUser = User.of(2L, "manager1", "pass", UserRole.MANAGER);
        Manager managerDomain = new Manager(10L, 2L, List.of(100L, 101L));

        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(managerRepository.findByUserId(2L)).thenReturn(Optional.of(managerDomain));

        // PathVariable 세팅: reservationId = 500
        Map<String, String> pathVariables = new HashMap<>();
        pathVariables.put("reservationId", "500");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, pathVariables);

        // 타 매장 예약이므로 existsByIdAndStoreIdIn => false
        when(reservationRepository.existsByIdAndStoreIdIn(500L, List.of(100L, 101L))).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> aspect.checkReservationAccess(joinPoint))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("매니저가 관리하는 매장에 접근할 경우 매장 접근 인가를 정상 통과한다")
    void checkStoreAccess_manager_success() throws Throwable {
        // given
        setLoginMember(2L, "manager1");
        User managerUser = User.of(2L, "manager1", "pass", UserRole.MANAGER);
        Manager managerDomain = new Manager(10L, 2L, List.of(100L, 101L));

        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(managerRepository.findByUserId(2L)).thenReturn(Optional.of(managerDomain));

        // storeId DTO Argument 세팅
        class DummyRequest {
            public Long storeId() {
                return 100L;
            }
        }
        when(joinPoint.getArgs()).thenReturn(new Object[]{new DummyRequest()});

        // when
        aspect.checkStoreAccess(joinPoint);

        // then
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("매니저가 관리하지 않는 매장에 접근할 경우 FORBIDDEN 예외를 던진다")
    void checkStoreAccess_manager_fail_forbidden() {
        // given
        setLoginMember(2L, "manager1");
        User managerUser = User.of(2L, "manager1", "pass", UserRole.MANAGER);
        Manager managerDomain = new Manager(10L, 2L, List.of(100L, 101L));

        when(userRepository.findById(2L)).thenReturn(Optional.of(managerUser));
        when(managerRepository.findByUserId(2L)).thenReturn(Optional.of(managerDomain));

        // storeId 파라미터 세팅: 999 (비인가 매장)
        request.setParameter("storeId", "999");

        // when & then
        assertThatThrownBy(() -> aspect.checkStoreAccess(joinPoint))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("일반 유저일 경우 예약 접근 시 FORBIDDEN 예외를 던진다")
    void checkReservationAccess_user_fail_forbidden() {
        // given
        setLoginMember(3L, "user1");
        User normalUser = User.of(3L, "user1", "pass", UserRole.USER);
        when(userRepository.findById(3L)).thenReturn(Optional.of(normalUser));

        // when & then
        assertThatThrownBy(() -> aspect.checkReservationAccess(joinPoint))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.FORBIDDEN);
    }
}
