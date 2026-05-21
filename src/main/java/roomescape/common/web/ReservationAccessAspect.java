package roomescape.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
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

@Aspect
@Component
public class ReservationAccessAspect {

    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final ReservationRepository reservationRepository;

    public ReservationAccessAspect(UserRepository userRepository,
                                   ManagerRepository managerRepository,
                                   ReservationRepository reservationRepository) {
        this.userRepository = userRepository;
        this.managerRepository = managerRepository;
        this.reservationRepository = reservationRepository;
    }

    @Around("@annotation(roomescape.common.web.CanAccessReservation)")
    public Object checkReservationAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        LoginMember loginMember = getLoginMember();
        User user = userRepository.findById(loginMember.id())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        if (user.getRole() == UserRole.ADMIN) {
            return joinPoint.proceed();
        }

        if (user.getRole() == UserRole.MANAGER) {
            Manager manager = managerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));

            Long reservationId = extractReservationId(joinPoint);
            if (reservationId == null) {
                throw new BusinessException(CommonErrorCode.NOT_FOUND);
            }

            boolean hasAccess = reservationRepository.existsByIdAndStoreIdIn(reservationId, manager.getManagedStoreIds());
            if (!hasAccess) {
                throw new BusinessException(CommonErrorCode.NOT_FOUND);
            }

            return joinPoint.proceed();
        }

        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    @Around("@annotation(roomescape.common.web.CanAccessStore)")
    public Object checkStoreAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        LoginMember loginMember = getLoginMember();
        User user = userRepository.findById(loginMember.id())
                .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));

        if (user.getRole() == UserRole.ADMIN) {
            return joinPoint.proceed();
        }

        if (user.getRole() == UserRole.MANAGER) {
            Manager manager = managerRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new BusinessException(CommonErrorCode.FORBIDDEN));

            Long storeId = extractStoreId(joinPoint);
            if (storeId == null || !manager.manages(storeId)) {
                throw new BusinessException(CommonErrorCode.FORBIDDEN);
            }

            return joinPoint.proceed();
        }

        throw new BusinessException(CommonErrorCode.FORBIDDEN);
    }

    private LoginMember getLoginMember() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        HttpServletRequest request = attributes.getRequest();
        LoginMember loginMember = (LoginMember) request.getAttribute("loginMember");
        if (loginMember == null) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }
        return loginMember;
    }

    private Long extractReservationId(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();

        // 1. Path Variable에서 추출
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables != null) {
            if (pathVariables.containsKey("reservationId")) {
                return Long.parseLong(pathVariables.get("reservationId"));
            }
            if (pathVariables.containsKey("id")) {
                return Long.parseLong(pathVariables.get("id"));
            }
        }

        // 2. Request Parameter에서 추출
        String resIdParam = request.getParameter("reservationId");
        if (resIdParam != null) {
            return Long.parseLong(resIdParam);
        }

        // 3. Method Args에서 Long 추출
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }

        return null;
    }

    private Long extractStoreId(ProceedingJoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();

        // 1. Path Variable에서 추출
        Map<String, String> pathVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVariables != null && pathVariables.containsKey("storeId")) {
            return Long.parseLong(pathVariables.get("storeId"));
        }

        // 2. Request Parameter에서 추출
        String storeIdParam = request.getParameter("storeId");
        if (storeIdParam != null) {
            return Long.parseLong(storeIdParam);
        }

        // 3. Method DTO Args에서 storeId 필드 추출
        for (Object arg : joinPoint.getArgs()) {
            if (arg != null) {
                try {
                    java.lang.reflect.Method method = arg.getClass().getMethod("storeId");
                    return (Long) method.invoke(arg);
                } catch (Exception ignored) {
                    try {
                        java.lang.reflect.Method getMethod = arg.getClass().getMethod("getStoreId");
                        return (Long) getMethod.invoke(arg);
                    } catch (Exception ignored2) {}
                }
            }
        }

        return null;
    }
}
