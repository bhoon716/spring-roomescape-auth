package roomescape.domain.auth.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.domain.auth.entity.RefreshToken;

@Repository
public class RefreshTokenJdbcRepository implements RefreshTokenRepository {

    private static final String FIND_BY_TOKEN_HASH_QUERY = """
            SELECT *
            FROM refresh_token
            WHERE token_hash = :token_hash;
            """;

    private static final String UPDATE_QUERY = """
            UPDATE refresh_token
            SET is_revoked = :is_revoked
            WHERE id = :id;
            """;

    private static final String REVOKE_IF_NOT_REVOKED_QUERY = """
            UPDATE refresh_token
            SET is_revoked = true
            WHERE id = :id
                AND is_revoked = false;
            """;

    private static final String REVOKE_ALL_BY_USER_ID_QUERY = """
            UPDATE refresh_token
            SET is_revoked = true
            WHERE user_id = :user_id
                AND is_revoked = false;
            """;

    private static final RowMapper<RefreshToken> REFRESH_TOKEN_ROW_MAPPER = (resultSet, rowNumber) -> RefreshToken
            .of(
                    resultSet.getLong("id"),
                    resultSet.getLong("user_id"),
                    resultSet.getString("token_hash"),
                    resultSet.getObject("expires_at", LocalDateTime.class),
                    resultSet.getBoolean("is_revoked"));

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public RefreshTokenJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("refresh_token")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        try {
            SqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("token_hash", tokenHash);

            RefreshToken refreshToken = jdbcTemplate.queryForObject(
                    FIND_BY_TOKEN_HASH_QUERY,
                    parameters,
                    REFRESH_TOKEN_ROW_MAPPER);

            return Optional.ofNullable(refreshToken);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("user_id", refreshToken.getUserId())
                .addValue("token_hash", refreshToken.getTokenHash())
                .addValue("expires_at", refreshToken.getExpiresAt())
                .addValue("is_revoked", refreshToken.isRevoked());

        Number key = simpleJdbcInsert.executeAndReturnKey(parameters);
        long generatedId = key.longValue();

        return RefreshToken.of(
                generatedId,
                refreshToken.getUserId(),
                refreshToken.getTokenHash(),
                refreshToken.getExpiresAt(),
                refreshToken.isRevoked());
    }

    @Override
    public void update(RefreshToken refreshToken) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("is_revoked", refreshToken.isRevoked())
                .addValue("id", refreshToken.getId());

        jdbcTemplate.update(UPDATE_QUERY, parameters);
    }

    @Override
    public boolean revokeIfNotRevoked(RefreshToken refreshToken) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", refreshToken.getId());

        return jdbcTemplate.update(REVOKE_IF_NOT_REVOKED_QUERY, parameters) == 1;
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("user_id", userId);

        jdbcTemplate.update(REVOKE_ALL_BY_USER_ID_QUERY, parameters);
    }
}
