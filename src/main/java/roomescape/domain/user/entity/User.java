package roomescape.domain.user.entity;

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
}
