package roomescape.domain.reservation.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.domain.reservation.entity.Reservation;
import roomescape.domain.reservationtime.entity.ReservationTime;
import roomescape.domain.store.entity.Store;
import roomescape.domain.theme.entity.Theme;

@Repository
public class ReservationJdbcRepository implements ReservationRepository {

    private static final String FIND_ALL_RESERVATIONS_WITH_TIME_QUERY = """
            SELECT
                r.id AS reservation_id,
                r.username AS username,
                r.date,
                s.id AS store_id,
                s.name AS store_name,
                t.id AS theme_id,
                t.name AS theme_name,
                t.description AS theme_description,
                t.thumbnail_url AS theme_thumbnail_url,
                rt.id AS time_id,
                rt.start_at
            FROM reservation AS r
            INNER JOIN store AS s
                ON r.store_id = s.id
            INNER JOIN reservation_time AS rt
                ON r.time_id = rt.id
            INNER JOIN theme AS t
                ON r.theme_id = t.id;
            """;

    private static final String FIND_ALL_RESERVATIONS_WITH_TIME_BY_USERNAME_QUERY = """
            SELECT
                r.id AS reservation_id,
                r.username AS username,
                r.date,
                s.id AS store_id,
                s.name AS store_name,
                t.id AS theme_id,
                t.name AS theme_name,
                t.description AS theme_description,
                t.thumbnail_url AS theme_thumbnail_url,
                rt.id AS time_id,
                rt.start_at
            FROM reservation AS r
            INNER JOIN store AS s
                ON r.store_id = s.id
            INNER JOIN reservation_time AS rt
                ON r.time_id = rt.id
            INNER JOIN theme AS t
                ON r.theme_id = t.id
            WHERE r.username = :username;
            """;

    private static final String FIND_ALL_RESERVATIONS_BY_STORE_IDS_QUERY = """
            SELECT
                r.id AS reservation_id,
                r.username AS username,
                r.date,
                s.id AS store_id,
                s.name AS store_name,
                t.id AS theme_id,
                t.name AS theme_name,
                t.description AS theme_description,
                t.thumbnail_url AS theme_thumbnail_url,
                rt.id AS time_id,
                rt.start_at
            FROM reservation AS r
            INNER JOIN store AS s
                ON r.store_id = s.id
            INNER JOIN reservation_time AS rt
                ON r.time_id = rt.id
            INNER JOIN theme AS t
                ON r.theme_id = t.id
            WHERE r.store_id IN (:storeIds);
            """;

    private static final String FIND_RESERVATION_WITH_TIME_AND_THEME_BY_ID_QUERY = """
            SELECT
                r.id AS reservation_id,
                r.username AS username,
                r.date,
                s.id AS store_id,
                s.name AS store_name,
                t.id AS theme_id,
                t.name AS theme_name,
                t.description AS theme_description,
                t.thumbnail_url AS theme_thumbnail_url,
                rt.id AS time_id,
                rt.start_at
            FROM reservation AS r
            INNER JOIN store AS s
                ON r.store_id = s.id
            INNER JOIN reservation_time AS rt
                ON r.time_id = rt.id
            INNER JOIN theme AS t
                ON r.theme_id = t.id
            WHERE r.id = :id;
            """;

    private static final String FIND_RESERVATION_WITH_TIME_AND_THEME_BY_ID_AND_USERNAME_QUERY = """
            SELECT
                r.id AS reservation_id,
                r.username AS username,
                r.date,
                s.id AS store_id,
                s.name AS store_name,
                t.id AS theme_id,
                t.name AS theme_name,
                t.description AS theme_description,
                t.thumbnail_url AS theme_thumbnail_url,
                rt.id AS time_id,
                rt.start_at
            FROM reservation AS r
            INNER JOIN store AS s
                ON r.store_id = s.id
            INNER JOIN reservation_time AS rt
                ON r.time_id = rt.id
            INNER JOIN theme AS t
                ON r.theme_id = t.id
            WHERE r.id = :id
                AND r.username = :username;
            """;

    private static final String FIND_RESERVATION_BY_ID_AND_STORE_IDS_QUERY = """
            SELECT
                r.id AS reservation_id,
                r.username AS username,
                r.date,
                s.id AS store_id,
                s.name AS store_name,
                t.id AS theme_id,
                t.name AS theme_name,
                t.description AS theme_description,
                t.thumbnail_url AS theme_thumbnail_url,
                rt.id AS time_id,
                rt.start_at
            FROM reservation AS r
            INNER JOIN store AS s
                ON r.store_id = s.id
            INNER JOIN reservation_time AS rt
                ON r.time_id = rt.id
            INNER JOIN theme AS t
                ON r.theme_id = t.id
            WHERE r.id = :id
                AND r.store_id IN (:storeIds);
            """;

    private static final String UPDATE_RESERVATION_BY_ID_QUERY = """
            UPDATE reservation
            SET
                store_id = :storeId,
                theme_id = :themeId,
                date = :date,
                time_id = :timeId
            WHERE id = :id;
            """;

    private static final String DELETE_RESERVATION_BY_ID_QUERY = """
            DELETE FROM reservation
            WHERE id = :id
            """;

    private static final String EXISTS_RESERVATION_BY_STORE_ID_AND_THEME_ID_AND_DATE_AND_TIME_ID_QUERY = """
            SELECT EXISTS (
                SELECT 1
                FROM reservation
                WHERE store_id = :storeId
                    AND theme_id = :themeId
                    AND date = :date
                    AND time_id = :timeId
            );
            """;

    private static final String EXISTS_RESERVATION_BY_STORE_ID_AND_THEME_ID_AND_DATE_AND_TIME_ID_AND_ID_NOT_QUERY = """
            SELECT EXISTS (
                SELECT 1
                FROM reservation
                WHERE store_id = :storeId
                    AND theme_id = :themeId
                    AND date = :date
                    AND time_id = :timeId
                    AND id <> :id
            );
            """;

    private static final String EXISTS_RESERVATION_BY_ID_AND_STORE_IDS_QUERY = """
            SELECT EXISTS (
                SELECT 1
                FROM reservation
                WHERE id = :id
                    AND store_id IN (:storeIds)
            );
            """;

    private static final RowMapper<Reservation> RESERVATION_ROW_MAPPER = (resultSet, rowNumber) -> Reservation.of(
            resultSet.getLong("reservation_id"),
            resultSet.getString("username"),
            new Store(
                    resultSet.getLong("store_id"),
                    resultSet.getString("store_name")
            ),
            Theme.of(
                    resultSet.getLong("theme_id"),
                    resultSet.getString("theme_name"),
                    resultSet.getString("theme_description"),
                    resultSet.getString("theme_thumbnail_url")),
            resultSet.getObject("date", LocalDate.class),
            ReservationTime.of(
                    resultSet.getLong("time_id"),
                    resultSet.getObject("start_at", LocalTime.class))
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public ReservationJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("reservation")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Reservation> findAll() {
        return jdbcTemplate.query(
                FIND_ALL_RESERVATIONS_WITH_TIME_QUERY,
                RESERVATION_ROW_MAPPER
        );
    }

    @Override
    public List<Reservation> findAllByUsername(String username) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("username", username);

        return jdbcTemplate.query(
                FIND_ALL_RESERVATIONS_WITH_TIME_BY_USERNAME_QUERY,
                parameters,
                RESERVATION_ROW_MAPPER
        );
    }

    @Override
    public List<Reservation> findAllByStoreIdIn(List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return List.of();
        }
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("storeIds", storeIds);

        return jdbcTemplate.query(
                FIND_ALL_RESERVATIONS_BY_STORE_IDS_QUERY,
                parameters,
                RESERVATION_ROW_MAPPER
        );
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id);

        try {
            Reservation reservation = jdbcTemplate.queryForObject(
                    FIND_RESERVATION_WITH_TIME_AND_THEME_BY_ID_QUERY,
                    parameters,
                    RESERVATION_ROW_MAPPER
            );

            return Optional.ofNullable(reservation);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Reservation> findByIdAndUsername(Long id, String username) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("username", username);

        try {
            Reservation reservation = jdbcTemplate.queryForObject(
                    FIND_RESERVATION_WITH_TIME_AND_THEME_BY_ID_AND_USERNAME_QUERY,
                    parameters,
                    RESERVATION_ROW_MAPPER
            );

            return Optional.ofNullable(reservation);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Reservation> findByIdAndStoreIdIn(Long id, List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return Optional.empty();
        }
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("storeIds", storeIds);

        try {
            Reservation reservation = jdbcTemplate.queryForObject(
                    FIND_RESERVATION_BY_ID_AND_STORE_IDS_QUERY,
                    parameters,
                    RESERVATION_ROW_MAPPER
            );

            return Optional.ofNullable(reservation);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Reservation save(Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation이 null 입니다.");
        }

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("username", reservation.getUsername())
                .addValue("store_id", reservation.getStore().getId())
                .addValue("theme_id", reservation.getTheme().getId())
                .addValue("date", reservation.getDate())
                .addValue("time_id", reservation.getTime().getId());

        Number key = simpleJdbcInsert.executeAndReturnKey(parameters);
        Long generatedId = key.longValue();

        return Reservation.of(generatedId, reservation.getUsername(), reservation.getStore(), reservation.getTheme(), reservation.getDate(),
                reservation.getTime());
    }

    @Override
    public int update(Long id, Reservation reservation) {
        if (reservation == null) {
            throw new IllegalArgumentException("reservation이 null 입니다.");
        }

        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("storeId", reservation.getStore().getId())
                .addValue("themeId", reservation.getTheme().getId())
                .addValue("date", reservation.getDate())
                .addValue("timeId", reservation.getTime().getId());

        return jdbcTemplate.update(UPDATE_RESERVATION_BY_ID_QUERY, parameters);
    }

    @Override
    public int deleteById(Long id) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id);

        return jdbcTemplate.update(
                DELETE_RESERVATION_BY_ID_QUERY,
                parameters
        );
    }

    @Override
    public boolean existsByStoreIdAndThemeIdAndDateAndTimeId(Long storeId, Long themeId, LocalDate date, Long timeId) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("storeId", storeId)
                .addValue("themeId", themeId)
                .addValue("date", date)
                .addValue("timeId", timeId);

        Boolean exists = jdbcTemplate.queryForObject(
                EXISTS_RESERVATION_BY_STORE_ID_AND_THEME_ID_AND_DATE_AND_TIME_ID_QUERY,
                parameters,
                Boolean.class
        );

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean existsByStoreIdAndThemeIdAndDateAndTimeIdAndIdNot(Long storeId, Long themeId, LocalDate date, Long timeId, Long id) {
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("storeId", storeId)
                .addValue("themeId", themeId)
                .addValue("date", date)
                .addValue("timeId", timeId)
                .addValue("id", id);

        Boolean exists = jdbcTemplate.queryForObject(
                EXISTS_RESERVATION_BY_STORE_ID_AND_THEME_ID_AND_DATE_AND_TIME_ID_AND_ID_NOT_QUERY,
                parameters,
                Boolean.class
        );

        return Boolean.TRUE.equals(exists);
    }

    @Override
    public boolean existsByIdAndStoreIdIn(Long id, List<Long> storeIds) {
        if (storeIds == null || storeIds.isEmpty()) {
            return false;
        }
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("storeIds", storeIds);

        Boolean exists = jdbcTemplate.queryForObject(
                EXISTS_RESERVATION_BY_ID_AND_STORE_IDS_QUERY,
                parameters,
                Boolean.class
        );

        return Boolean.TRUE.equals(exists);
    }
}
