package roomescape.domain.store.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import roomescape.domain.store.entity.Manager;

@Repository
public class ManagerJdbcRepository implements ManagerRepository {

    private static final String FIND_MANAGER_BY_USER_ID_QUERY = """
            SELECT id, user_id
            FROM manager
            WHERE user_id = :userId;
            """;

    private static final String FIND_MANAGED_STORE_IDS_QUERY = """
            SELECT store_id
            FROM manager_store
            WHERE manager_id = :managerId;
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert managerInsert;
    private final SimpleJdbcInsert managerStoreInsert;

    public ManagerJdbcRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.managerInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("manager")
                .usingGeneratedKeyColumns("id");
        this.managerStoreInsert = new SimpleJdbcInsert(jdbcTemplate.getJdbcTemplate())
                .withTableName("manager_store");
    }

    @Override
    public Optional<Manager> findByUserId(Long userId) {
        SqlParameterSource parameters = new MapSqlParameterSource().addValue("userId", userId);
        try {
            Long managerId = jdbcTemplate.queryForObject(
                    FIND_MANAGER_BY_USER_ID_QUERY,
                    parameters,
                    (rs, rowNum) -> rs.getLong("id")
            );

            SqlParameterSource storeParams = new MapSqlParameterSource().addValue("managerId", managerId);
            List<Long> storeIds = jdbcTemplate.query(
                    FIND_MANAGED_STORE_IDS_QUERY,
                    storeParams,
                    (rs, rowNum) -> rs.getLong("store_id")
            );

            return Optional.of(new Manager(managerId, userId, storeIds));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    @Override
    public Manager save(Manager manager) {
        if (manager == null) {
            throw new IllegalArgumentException("manager가 null 입니다.");
        }
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("user_id", manager.getUserId());
        Number key = managerInsert.executeAndReturnKey(parameters);
        Long managerId = key.longValue();

        for (Long storeId : manager.getManagedStoreIds()) {
            SqlParameterSource mappingParams = new MapSqlParameterSource()
                    .addValue("manager_id", managerId)
                    .addValue("store_id", storeId);
            managerStoreInsert.execute(mappingParams);
        }

        return new Manager(managerId, manager.getUserId(), manager.getManagedStoreIds());
    }
}
