package roomescape.domain.user.exception;

import org.springframework.http.HttpStatus;
import roomescape.common.exception.ErrorCode;

public enum UserErrorCode implements ErrorCode {

    DUPLICATED_USERNAME(HttpStatus.CONFLICT, "중복된 username 입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "user를 찾을 수 없습니다."),
    USERNAME_OR_PASSWORD_NOT_MATCHES(HttpStatus.UNAUTHORIZED, "username 또는 password가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    UserErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }


    @Override
    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
