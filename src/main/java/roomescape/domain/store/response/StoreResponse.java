package roomescape.domain.store.response;

import roomescape.domain.store.entity.Store;

public record StoreResponse(
        Long id,
        String name
) {
    public static StoreResponse from(Store store) {
        return new StoreResponse(store.getId(), store.getName());
    }
}
