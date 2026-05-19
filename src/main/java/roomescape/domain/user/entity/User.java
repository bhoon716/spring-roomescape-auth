package roomescape.domain.user.entity;

import java.util.Objects;

public class User {

    private final Long id;

    private final String username;

    private final String password;

    private final UserRole role;

    private User(Long id, String username, String password, UserRole role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public static User createUser(String username, String password) {
        return new User(null, username, password, UserRole.USER);
    }

    public static User of(Long id, String username, String password, UserRole role) {
        return new User(id, username, password, role);
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        User that = (User) other;

        if (this.id == null || that.id == null) {
            return false;
        }

        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        if (id == null) {
            return System.identityHashCode(this);
        }
        return id.hashCode();
    }
}
