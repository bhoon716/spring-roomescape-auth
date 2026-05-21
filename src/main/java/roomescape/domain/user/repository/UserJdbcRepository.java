package roomescape.domain.user.repository;

import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.domain.user.entity.User;
import roomescape.domain.user.entity.UserRole;

@Repository
public class UserJdbcRepository implements UserRepository {

    private static final String FIND_USER_BY_USERNAME_QUERY = """
            SELECT *
            FROM users
            WHERE username = :username;
            """;

    private static final String FIND_USER_BY_ID_QUERY = """
            SELECT *
            FROM users
            WHERE id = :id;
            """;

    private static final String EXISTS_BY_USERNAME_QUERY = """
            SELECT EXISTS (
                SELECT 1
                FROM users
                WHERE username = :username
            );
            """;

    private static final RowMapper<User> USER_ROW_MAPPER = (resultSet, rowNumber) -> User.of(
            resultSet.getLong("id"),
            resultSet.getString("username"),
            resultSet.getString("password"),
            UserRole.valueOf(resultSet.getString("role")));

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public UserJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("users")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<User> findByUsername(String username) {
        try {
            SqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("username", username);

            User user = jdbcTemplate.queryForObject(
                    FIND_USER_BY_USERNAME_QUERY,
                    parameters,
                    USER_ROW_MAPPER);

            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public User save(User user) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("username", user.getUsername())
                .addValue("password", user.getPassword())
                .addValue("role", user.getRole().name());

        Number key = simpleJdbcInsert.executeAndReturnKey(parameters);
        long generatedId = key.longValue();

        return User.of(generatedId, user.getUsername(), user.getPassword(), user.getRole());
    }

    @Override
    public boolean existsByUsername(String username) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("username", username);

        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(EXISTS_BY_USERNAME_QUERY, parameters, Boolean.class));
    }

    @Override
    public Optional<User> findById(Long id) {
        try {
            SqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("id", id);

            User user = jdbcTemplate.queryForObject(
                    FIND_USER_BY_ID_QUERY,
                    parameters,
                    USER_ROW_MAPPER);

            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }
}
