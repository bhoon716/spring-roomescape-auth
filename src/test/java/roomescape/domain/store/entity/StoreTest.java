package roomescape.domain.store.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StoreTest {

    @Test
    @DisplayName("매장을 ID와 이름으로 정상적으로 생성한다")
    void create_store_with_id_and_name_success() {
        // given
        Long id = 1L;
        String name = "강남점";

        // when
        Store store = new Store(id, name);

        // then
        assertThat(store.getId()).isEqualTo(id);
        assertThat(store.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("ID가 없이 매장 이름으로만 매장을 정상적으로 생성하면 ID는 null이다")
    void create_store_without_id_success() {
        // given
        String name = "홍대점";

        // when
        Store store = new Store(name);

        // then
        assertThat(store.getId()).isNull();
        assertThat(store.getName()).isEqualTo(name);
    }
}
