CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Token Info
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    token_type VARCHAR(20) NOT NULL,  -- REFRESH

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,

    -- Status
    revoked BOOLEAN NOT NULL DEFAULT FALSE,

    -- Client Info
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    device_name VARCHAR(100),

    -- Foreign Keys
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE
);