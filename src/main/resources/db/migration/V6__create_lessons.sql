CREATE TABLE lessons (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Relation
    section_id BIGINT NOT NULL,

    -- Info
    title VARCHAR(200) NOT NULL,
    order_index INTEGER NOT NULL,

    -- Type
    type VARCHAR(30) NOT NULL,  -- VIDEO, DOCUMENT, TEXT, QUIZ, ASSIGNMENT

    -- Content
    content TEXT,
    file_url VARCHAR(500),
    mime_type VARCHAR(100),
    file_size_bytes BIGINT,
    file_name VARCHAR(500),

    -- Video specific
    duration_seconds INTEGER,
    external_video_url VARCHAR(500),

    -- Settings
    downloadable BOOLEAN NOT NULL DEFAULT TRUE,
    free BOOLEAN NOT NULL DEFAULT TRUE,

    -- Additional
    description TEXT,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_lesson_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_lesson_section FOREIGN KEY (section_id)
        REFERENCES course_sections (id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX idx_lesson_org_id ON lessons (organization_id);
CREATE INDEX idx_lesson_section ON lessons (section_id);
CREATE INDEX idx_lesson_order ON lessons (order_index);
CREATE INDEX idx_lesson_type ON lessons (type);