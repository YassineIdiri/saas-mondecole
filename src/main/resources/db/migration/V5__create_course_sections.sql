CREATE TABLE course_sections (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Relation
    course_id BIGINT NOT NULL,

    -- Info
    title VARCHAR(200) NOT NULL,
    description TEXT,
    order_index INTEGER NOT NULL,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_section_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_section_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE
);