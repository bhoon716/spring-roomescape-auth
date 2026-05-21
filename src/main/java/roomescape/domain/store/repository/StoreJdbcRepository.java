package roomescape.domain.store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.domain.store.entity.Store;

@Repository
public class StoreJdbcRepository implements StoreRepository {

    private static final String FIND_ALL_STORES_QUERY = """
            SELECT id, name
            FROM store;
            """;

    private static final String FIND_STORE_BY_ID_QUERY = """
            SELECT id, name
            FROM store
            WHERE id = :id;
            """;

    private static final String DELETE_STORE_BY_ID_QUERY = """
            DELETE FROM store
            WHERE id = :id;
            """;

    private static final RowMapper<Store> STORE_ROW_MAPPER = (resultSet, rowNumber) -> new Store(
            resultSet.getLong("id"),
            resultSet.getString("name")
    );

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert simpleJdbcInsert;

    public StoreJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("store")
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Store> findAll() {
        return jdbcTemplate.query(FIND_ALL_STORES_QUERY, STORE_ROW_MAPPER);
    }

    @Override
    public Optional<Store> findById(Long id) {
        SqlParameterSource parameters = new MapSqlParameterSource().addValue("id", id);
        try {
            Store store = jdbcTemplate.queryForObject(FIND_STORE_BY_ID_QUERY, parameters, STORE_ROW_MAPPER);
            return Optional.ofNullable(store);
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Store save(Store store) {
        if (store == null) {
            throw new IllegalArgumentException("store이 null 입니다.");
        }
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("name", store.getName());
        Number key = simpleJdbcInsert.executeAndReturnKey(parameters);
        return new Store(key.longValue(), store.getName());
    }

    @Override
    public int deleteById(Long id) {
        SqlParameterSource parameters = new MapSqlParameterSource().addValue("id", id);
        return jdbcTemplate.update(DELETE_STORE_BY_ID_QUERY, parameters);
    }
}
