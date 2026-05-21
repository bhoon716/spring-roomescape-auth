package roomescape.domain.store.entity;

public class Store {

    private final Long id;
    private final String name;

    public Store(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Store(String name) {
        this(null, name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
