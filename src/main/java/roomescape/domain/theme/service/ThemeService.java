package roomescape.domain.theme.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import roomescape.common.exception.BusinessException;
import roomescape.domain.theme.entity.Theme;
import roomescape.domain.theme.exception.ThemeErrorCode;
import roomescape.domain.theme.repository.ThemeRepository;
import roomescape.domain.theme.request.ThemeCreateRequest;
import roomescape.domain.theme.request.ThemeUpdateRequest;
import roomescape.domain.theme.response.PopularThemeResponse;
import roomescape.domain.theme.response.PopularThemesResponse;
import roomescape.domain.theme.response.ThemeReservationTimeResponse;
import roomescape.domain.theme.response.ThemeReservationTimesResponse;
import roomescape.domain.theme.response.ThemeResponse;
import roomescape.domain.theme.response.ThemesResponse;

@Service
@Transactional(readOnly = true)
public class ThemeService {

    private final ThemeRepository themeRepository;
    private final Clock clock;

    public ThemeService(ThemeRepository themeRepository, Clock clock) {
        this.themeRepository = themeRepository;
        this.clock = clock;
    }

    public ThemesResponse findAllThemes() {
        List<ThemeResponse> themes = themeRepository.findAll().stream()
                .map(ThemeResponse::from)
                .toList();

        return new ThemesResponse(themes);
    }

    public ThemeReservationTimesResponse findAllThemeReservationTimes(Long themeId, LocalDate date) {
        if (!themeRepository.existsById(themeId)) {
            throw new BusinessException(ThemeErrorCode.THEME_NOT_FOUND);
        }
        List<ThemeReservationTimeResponse> times = themeRepository.findAllReservationTimesByThemeIdAndDate(themeId,
                        date)
                .stream()
                .map(ThemeReservationTimeResponse::from)
                .toList();

        return new ThemeReservationTimesResponse(times);
    }

    public PopularThemesResponse findPopularThemes(Integer period, Integer limit) {
        LocalDate today = LocalDate.now(clock);
        LocalDate startDate = today.minusDays(period);
        LocalDate endDate = today.minusDays(1);

        List<PopularThemeResponse> themes = themeRepository.findPopularThemes(startDate, endDate, limit)
                .stream()
                .map(PopularThemeResponse::from)
                .toList();

        return new PopularThemesResponse(themes);
    }

    @Transactional
    public ThemeResponse saveTheme(ThemeCreateRequest request) {
        validateThemeNameNotDuplicated(request.name());

        Theme theme = Theme.create(request.name(), request.description(), request.thumbnailUrl());
        Theme savedTheme = themeRepository.save(theme);

        return ThemeResponse.from(savedTheme);
    }

    @Transactional
    public ThemeResponse updateTheme(Long id, ThemeUpdateRequest request) {
        Theme theme = findThemeByIdOrThrow(id);

        validateThemeNameNotDuplicated(request.name(), id);

        Theme updatedTheme = theme.update(request.name(), request.description(), request.thumbnailUrl());
        themeRepository.update(id, updatedTheme);

        return ThemeResponse.from(updatedTheme);
    }

    @Transactional
    public void deleteThemeById(Long themeId) {
        try {
            int deletedCount = themeRepository.deleteById(themeId);
            if (deletedCount == 0) {
                throw new BusinessException(ThemeErrorCode.THEME_NOT_FOUND);
            }
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ThemeErrorCode.THEME_DELETE_CONFLICT, exception);
        }
    }

    private Theme findThemeByIdOrThrow(Long themeId) {
        return themeRepository.findById(themeId)
                .orElseThrow(() -> new BusinessException(ThemeErrorCode.THEME_NOT_FOUND));
    }

    private void validateThemeNameNotDuplicated(String name) {
        if (themeRepository.existsByName(name)) {
            throw new BusinessException(ThemeErrorCode.THEME_DUPLICATE);
        }
    }

    private void validateThemeNameNotDuplicated(String name, Long excludeId) {
        if (themeRepository.existsByNameAndIdNot(name, excludeId)) {
            throw new BusinessException(ThemeErrorCode.THEME_DUPLICATE);
        }
    }
}
