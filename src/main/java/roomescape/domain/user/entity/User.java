package roomescape.domain.user.entity;

public class User {

    private final Long id;

    private final String username;

    private final String password;

    private User(Long id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public static User create(String username, String password) {
        return new User(null, username, password);
    }

    public static User of(Long id, String username, String password) {
        return new User(id, username, password);
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
}
