CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Credentials
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),

    -- Personal Info
    first_name VARCHAR(100),
    last_name VARCHAR(100),

    -- Role
    role VARCHAR(50) NOT NULL,  -- ADMIN, TEACHER, STUDENT

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    last_login_at TIMESTAMP,

    -- Foreign Key
    CONSTRAINT fk_user_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_user_username ON users (username);
CREATE INDEX idx_user_org_id ON users (organization_id);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_role ON users (role);
CREATE INDEX idx_user_active ON users (active);

-- Unique constraint: username unique per organization
CREATE UNIQUE INDEX uk_user_org_username ON users (organization_id, username);

-- Optional: email unique per organization (if needed)
CREATE INDEX idx_user_org_email ON users (organization_id, email);