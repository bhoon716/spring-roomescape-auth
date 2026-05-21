package roomescape.domain.store.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ManagerTest {

    @Test
    @DisplayName("매니저가 관리하는 매장에 대해 manages 호출 시 true를 반환한다")
    void manages_returns_true_for_managed_store() {
        // given
        Manager manager = new Manager(1L, 10L, List.of(1L, 2L));

        // when & then
        assertThat(manager.manages(1L)).isTrue();
        assertThat(manager.manages(2L)).isTrue();
    }

    @Test
    @DisplayName("매니저가 관리하지 않는 매장에 대해 manages 호출 시 false를 반환한다")
    void manages_returns_false_for_unmanaged_store() {
        // given
        Manager manager = new Manager(1L, 10L, List.of(1L, 2L));

        // when & then
        assertThat(manager.manages(3L)).isFalse();
    }

    @Test
    @DisplayName("생성자에 전달된 managedStoreIds 리스트가 외부 변경에 안전하도록 불변 복사본을 만든다")
    void constructor_defensively_copies_store_ids() {
        // given
        List<Long> storeIds = new ArrayList<>();
        storeIds.add(1L);
        storeIds.add(2L);

        Manager manager = new Manager(1L, 10L, storeIds);

        // when
        storeIds.add(3L);

        // then
        assertThat(manager.getManagedStoreIds()).hasSize(2);
        assertThat(manager.manages(3L)).isFalse();
    }
}
