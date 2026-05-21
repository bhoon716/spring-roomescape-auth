CREATE TABLE users
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role     VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_user_username UNIQUE (username)
);

CREATE TABLE refresh_token
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(255) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    is_revoked  BOOLEAN      NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    CONSTRAINT uk_refresh_token_token_hash UNIQUE (token_hash)
);

CREATE TABLE store
(
    id   BIGINT       NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_store_name UNIQUE (name)
);

CREATE TABLE manager
(
    id      BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
);

CREATE TABLE manager_store
(
    manager_id BIGINT NOT NULL,
    store_id   BIGINT NOT NULL,
    PRIMARY KEY (manager_id, store_id),
    FOREIGN KEY (manager_id) REFERENCES manager (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT,
    FOREIGN KEY (store_id) REFERENCES store (id)
        ON DELETE CASCADE
        ON UPDATE RESTRICT
);

CREATE TABLE reservation_time
(
    id       BIGINT NOT NULL AUTO_INCREMENT,
    start_at TIME   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_reservation_time_start_at UNIQUE (start_at)
);

CREATE TABLE theme
(
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(255) NOT NULL,
    description   VARCHAR(255) NOT NULL,
    thumbnail_url VARCHAR(255) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_theme_name UNIQUE (name)
);

CREATE TABLE reservation
(
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    username VARCHAR(255) NOT NULL,
    store_id BIGINT       NOT NULL,
    theme_id BIGINT       NOT NULL,
    date     DATE         NOT NULL,
    time_id  BIGINT       NOT NULL,
    PRIMARY KEY (id),
    FOREIGN KEY (store_id) REFERENCES store (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    FOREIGN KEY (time_id) REFERENCES reservation_time (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    FOREIGN KEY (theme_id) REFERENCES theme (id)
        ON DELETE RESTRICT
        ON UPDATE RESTRICT,
    CONSTRAINT uk_reservation_slot UNIQUE (store_id, theme_id, date, time_id)
);
