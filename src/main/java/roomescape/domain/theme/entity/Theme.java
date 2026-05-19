package roomescape.domain.theme.entity;

import java.util.Objects;

public class Theme {

    private final Long id;

    private final String name;

    private final String description;

    private final String thumbnailUrl;

    private Theme(Long id, String name, String description, String thumbnailUrl) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
    }

    public static Theme create(String name, String description, String thumbnailUrl) {
        return new Theme(null, name, description, thumbnailUrl);
    }

    public static Theme of(Long id, String name, String description, String thumbnailUrl) {
        return new Theme(id, name, description, thumbnailUrl);
    }

    public Theme assignId(Long id) {
        validateAssignableId(id);
        return new Theme(id, this.name, this.description, this.thumbnailUrl);
    }

    private void validateAssignableId(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("id는 양수여야 합니다.");
        }

        if (this.id != null) {
            throw new IllegalStateException("이미 id가 할당된 테마입니다.");
        }
    }

    public Theme update(String name, String description, String thumbnailUrl) {
        return new Theme(this.id, name, description, thumbnailUrl);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        if (this.id == null) {
            return false;
        }
        Theme theme = (Theme) other;
        return Objects.equals(id, theme.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
