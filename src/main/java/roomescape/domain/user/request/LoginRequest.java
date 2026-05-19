package roomescape.domain.user.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record LoginRequest(
        @NotBlank(message = "username은 필수입니다.")
        @Length(min = 3, max = 10, message = "username은 최소 3자, 최대 10자의 문자열만 가능합니다.")
        String username,

        @NotBlank
        @Length(min = 9, max = 20, message = "password는 최소 9자, 최대 20자의 문자열만 가능합니다.")
        String password
) {
}
