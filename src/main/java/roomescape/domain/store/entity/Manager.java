package roomescape.domain.store.entity;

import java.util.List;

public class Manager {

    private final Long id;
    private final Long userId;
    private final List<Long> managedStoreIds;

    public Manager(Long id, Long userId, List<Long> managedStoreIds) {
        this.id = id;
        this.userId = userId;
        this.managedStoreIds = List.copyOf(managedStoreIds);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public List<Long> getManagedStoreIds() {
        return managedStoreIds;
    }

    public boolean manages(Long storeId) {
        return managedStoreIds.contains(storeId);
    }
}
