package roomescape.domain.theme.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import roomescape.common.exception.BusinessException;
import roomescape.common.exception.CommonErrorCode;
import roomescape.domain.theme.entity.Theme;
import roomescape.domain.theme.exception.ThemeErrorCode;
import roomescape.domain.theme.repository.PopularThemeResult;
import roomescape.domain.theme.repository.ThemeRepository;
import roomescape.domain.theme.repository.ThemeReservationTimeResult;
import roomescape.domain.theme.request.ThemeCreateRequest;
import roomescape.domain.theme.request.ThemeUpdateRequest;
import roomescape.domain.theme.response.PopularThemesResponse;
import roomescape.domain.theme.response.ThemeReservationTimesResponse;
import roomescape.domain.theme.response.ThemeResponse;
import roomescape.domain.theme.response.ThemesResponse;

class ThemeServiceTest {

    private ThemeRepository themeRepository;
    private Clock clock;
    private ThemeService service;

    @BeforeEach
    void setUp() {
        themeRepository = mock(ThemeRepository.class);
        clock = Clock.fixed(Instant.parse("2026-05-21T18:00:00Z"), ZoneId.of("UTC")); // 2026-05-21
        service = new ThemeService(themeRepository, clock);
    }

    @Test
    @DisplayName("findAllThemes 호출 시 모든 테마 목록을 반환한다")
    void findAllThemes_success() {
        // given
        Theme theme1 = Theme.of(1L, "공포테마", "무서운 테마", "http://image1.png");
        Theme theme2 = Theme.of(2L, "탈출테마", "탈출하는 테마", "http://image2.png");
        when(themeRepository.findAll()).thenReturn(List.of(theme1, theme2));

        // when
        ThemesResponse response = service.findAllThemes();

        // then
        assertThat(response.themes()).hasSize(2);
        assertThat(response.themes().get(0).name()).isEqualTo("공포테마");
        assertThat(response.themes().get(1).name()).isEqualTo("탈출테마");
    }

    @Test
    @DisplayName("findAllThemeReservationTimes 호출 시 정상적으로 테마 예약 가능 시간을 반환한다")
    void findAllThemeReservationTimes_success() {
        // given
        Long themeId = 1L;
        LocalDate date = LocalDate.of(2026, 5, 22); // 오늘(21일) 이후 날짜
        when(themeRepository.existsById(themeId)).thenReturn(true);

        ThemeReservationTimeResult result1 = new ThemeReservationTimeResult(10L, LocalTime.of(13, 0), true);
        ThemeReservationTimeResult result2 = new ThemeReservationTimeResult(11L, LocalTime.of(15, 0), false);
        when(themeRepository.findAllReservationTimesByThemeIdAndDate(themeId, date)).thenReturn(List.of(result1, result2));

        // when
        ThemeReservationTimesResponse response = service.findAllThemeReservationTimes(themeId, date);

        // then
        assertThat(response.times()).hasSize(2);
        assertThat(response.times().get(0).id()).isEqualTo(10L);
        assertThat(response.times().get(0).isAvailable()).isTrue();
        assertThat(response.times().get(1).isAvailable()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 테마 아이디로 예약 시간을 조회하면 THEME_NOT_FOUND 예외를 던진다")
    void findAllThemeReservationTimes_fail_notFound() {
        // given
        Long themeId = 999L;
        LocalDate date = LocalDate.of(2026, 5, 22);
        when(themeRepository.existsById(themeId)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> service.findAllThemeReservationTimes(themeId, date))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_NOT_FOUND);
    }

    @Test
    @DisplayName("오늘 이전 날짜로 예약 시간을 조회하면 VALIDATION_FAILED 예외를 던진다")
    void findAllThemeReservationTimes_fail_pastDate() {
        // given
        Long themeId = 1L;
        LocalDate date = LocalDate.of(2026, 5, 20); // 오늘(21일) 이전
        when(themeRepository.existsById(themeId)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.findAllThemeReservationTimes(themeId, date))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("findPopularThemes 호출 시 정상적인 파라미터가 들어오면 인기 테마 목록을 반환한다")
    void findPopularThemes_success() {
        // given
        int period = 7;
        int limit = 5;
        LocalDate today = LocalDate.of(2026, 5, 21);
        LocalDate startDate = today.minusDays(period); // 2026-05-14
        LocalDate endDate = today.minusDays(1);        // 2026-05-20

        PopularThemeResult result = new PopularThemeResult(1L, "인기테마", "설명", "url", 1);
        when(themeRepository.findPopularThemes(startDate, endDate, limit)).thenReturn(List.of(result));

        // when
        PopularThemesResponse response = service.findPopularThemes(period, limit);

        // then
        assertThat(response.popularThemes()).hasSize(1);
        assertThat(response.popularThemes().get(0).name()).isEqualTo("인기테마");
        assertThat(response.popularThemes().get(0).rank()).isEqualTo(1);
    }

    @Test
    @DisplayName("인기 테마 조회 시 period나 limit가 잘못되면 VALIDATION_FAILED 예외를 던진다")
    void findPopularThemes_fail_invalidParams() {
        // when & then
        assertThatThrownBy(() -> service.findPopularThemes(0, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED);

        assertThatThrownBy(() -> service.findPopularThemes(7, -1))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED);

        assertThatThrownBy(() -> service.findPopularThemes(null, 5))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED);
    }

    @Test
    @DisplayName("saveTheme 호출 시 새로운 테마를 정상적으로 저장하고 반환한다")
    void saveTheme_success() {
        // given
        ThemeCreateRequest request = new ThemeCreateRequest("신규테마", "설명", "http://image.png");
        when(themeRepository.existsByName(request.name())).thenReturn(false);

        Theme savedTheme = Theme.of(10L, "신규테마", "설명", "http://image.png");
        when(themeRepository.save(any(Theme.class))).thenReturn(savedTheme);

        // when
        ThemeResponse response = service.saveTheme(request);

        // then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("신규테마");
        verify(themeRepository).save(any(Theme.class));
    }

    @Test
    @DisplayName("중복된 테마 이름으로 저장 시도 시 THEME_DUPLICATE 예외를 던진다")
    void saveTheme_fail_duplicate() {
        // given
        ThemeCreateRequest request = new ThemeCreateRequest("중복테마", "설명", "http://image.png");
        when(themeRepository.existsByName(request.name())).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.saveTheme(request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_DUPLICATE);

        verify(themeRepository, never()).save(any(Theme.class));
    }

    @Test
    @DisplayName("updateTheme 호출 시 정상적으로 테마 정보를 수정하고 반환한다")
    void updateTheme_success() {
        // given
        Long id = 1L;
        ThemeUpdateRequest request = new ThemeUpdateRequest("수정테마", "수정설명", "http://new.png");
        Theme existingTheme = Theme.of(id, "기존테마", "설명", "http://old.png");

        when(themeRepository.findById(id)).thenReturn(Optional.of(existingTheme));
        when(themeRepository.existsByNameAndIdNot(request.name(), id)).thenReturn(false);

        // when
        ThemeResponse response = service.updateTheme(id, request);

        // then
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("수정테마");
        assertThat(response.description()).isEqualTo("수정설명");
        verify(themeRepository).update(any(Long.class), any(Theme.class));
    }

    @Test
    @DisplayName("존재하지 않는 테마를 수정하려고 하면 THEME_NOT_FOUND 예외를 던진다")
    void updateTheme_fail_notFound() {
        // given
        Long id = 999L;
        ThemeUpdateRequest request = new ThemeUpdateRequest("수정테마", "수정설명", "http://new.png");
        when(themeRepository.findById(id)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> service.updateTheme(id, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 테마의 이름과 중복되는 이름으로 수정하려고 하면 THEME_DUPLICATE 예외를 던진다")
    void updateTheme_fail_duplicate() {
        // given
        Long id = 1L;
        ThemeUpdateRequest request = new ThemeUpdateRequest("중복이름", "수정설명", "http://new.png");
        Theme existingTheme = Theme.of(id, "기존테마", "설명", "http://old.png");

        when(themeRepository.findById(id)).thenReturn(Optional.of(existingTheme));
        when(themeRepository.existsByNameAndIdNot(request.name(), id)).thenReturn(true);

        // when & then
        assertThatThrownBy(() -> service.updateTheme(id, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_DUPLICATE);
    }

    @Test
    @DisplayName("deleteThemeById 호출 시 정상적으로 테마를 삭제한다")
    void deleteThemeById_success() {
        // given
        Long id = 1L;
        when(themeRepository.deleteById(id)).thenReturn(1);

        // when
        service.deleteThemeById(id);

        // then
        verify(themeRepository).deleteById(id);
    }

    @Test
    @DisplayName("존재하지 않는 테마를 삭제하려고 하면 THEME_NOT_FOUND 예외를 던진다")
    void deleteThemeById_fail_notFound() {
        // given
        Long id = 999L;
        when(themeRepository.deleteById(id)).thenReturn(0);

        // when & then
        assertThatThrownBy(() -> service.deleteThemeById(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_NOT_FOUND);
    }

    @Test
    @DisplayName("삭제 시 무결성 제약조건 위반 발생 시 THEME_DELETE_CONFLICT 예외를 던진다")
    void deleteThemeById_fail_conflict() {
        // given
        Long id = 1L;
        when(themeRepository.deleteById(id)).thenThrow(new DataIntegrityViolationException("foreign key violation"));

        // when & then
        assertThatThrownBy(() -> service.deleteThemeById(id))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ThemeErrorCode.THEME_DELETE_CONFLICT);
    }
}
