CREATE TABLE courses (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Author
    author_id BIGINT NOT NULL,

    -- Basic Info
    title VARCHAR(200) NOT NULL,
    slug VARCHAR(200) NOT NULL,
    summary VARCHAR(500),
    description TEXT,

    -- Categorization
    category VARCHAR(100),
    tags TEXT[],
    level VARCHAR(30),  -- BEGINNER, INTERMEDIATE, ADVANCED

    -- Additional Info
    estimated_hours INTEGER,
    thumbnail_url VARCHAR(500),
    language VARCHAR(10) DEFAULT 'fr',

    -- Publishing
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,

    -- Educational
    objectives TEXT,
    prerequisites TEXT,

    -- Status
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_course_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_course_author FOREIGN KEY (author_id)
        REFERENCES users (id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_course_org_id ON courses (organization_id);
CREATE INDEX idx_course_author ON courses (author_id);
CREATE INDEX idx_course_category ON courses (category);
CREATE INDEX idx_course_published ON courses (published);
CREATE INDEX idx_course_slug ON courses (slug);