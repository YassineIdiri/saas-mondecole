CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,

    -- Basic Info
    name VARCHAR(200) NOT NULL,
    slug VARCHAR(100) NOT NULL UNIQUE,
    invitation_code VARCHAR(50) NOT NULL UNIQUE,

    -- Contact Info
    description TEXT,
    email VARCHAR(200),
    phone VARCHAR(50),
    address VARCHAR(500),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    -- Additional Info
    logo_url VARCHAR(500),
    website VARCHAR(500),
    registration_number VARCHAR(50),
    activity_declaration_number VARCHAR(50),

    -- Type & Plan
    type VARCHAR(50) NOT NULL,  -- TRAINING_CENTER, UNIVERSITY, SCHOOL, COMPANY, OTHER
    plan VARCHAR(30) NOT NULL DEFAULT 'FREE',  -- FREE, BASIC, PREMIUM, ENTERPRISE

    -- Limits
    max_users INTEGER NOT NULL DEFAULT 50,
    max_storage_mb INTEGER NOT NULL DEFAULT 1000,

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_org_slug ON organizations (slug);
CREATE INDEX idx_org_invitation_code ON organizations (invitation_code);
CREATE INDEX idx_org_active ON organizations (active);