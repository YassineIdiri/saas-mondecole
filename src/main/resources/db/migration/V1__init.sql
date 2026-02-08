CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    username VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,

    role VARCHAR(30) NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP
);

CREATE UNIQUE INDEX uk_users_username ON users (username);
CREATE UNIQUE INDEX uk_users_email ON users (email);
CREATE INDEX idx_user_username ON users (username);
CREATE INDEX idx_user_email ON users (email);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,

    token_hash VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,

    token_type VARCHAR(20) NOT NULL,

    created_at TIMESTAMP NOT NULL,
    last_used_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,

    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_name VARCHAR(100)
);

CREATE UNIQUE INDEX uk_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_expires ON refresh_tokens (expires_at);

ALTER TABLE refresh_tokens
ADD CONSTRAINT fk_refresh_user
FOREIGN KEY (user_id)
REFERENCES users (id)
ON DELETE CASCADE;
