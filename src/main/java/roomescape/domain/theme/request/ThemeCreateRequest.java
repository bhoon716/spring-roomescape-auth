package roomescape.domain.theme.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ThemeCreateRequest(
        @NotBlank(message = "테마 이름은 필수입니다.")
        @Length(min = 2, max = 20, message = "테마 이름은 최소 2자, 최대 20자의 문자열만 가능합니다.")
        String name,

        @NotBlank(message = "테마 설명은 필수입니다.")
        String description,

        @NotBlank(message = "테마 썸네일 URL은 필수입니다.")
        String thumbnailUrl
) {
}
