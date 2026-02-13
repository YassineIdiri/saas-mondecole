CREATE TABLE lesson_progress (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Relations
    student_id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,

    -- Progress
    progress_percent INTEGER NOT NULL DEFAULT 0,
    last_position_seconds INTEGER,

    -- Completion
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,

    -- Stats
    first_accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    view_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_progress_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_progress_lesson FOREIGN KEY (lesson_id)
        REFERENCES lessons (id) ON DELETE CASCADE,

    -- Unique constraint
    CONSTRAINT uk_progress_student_lesson UNIQUE (student_id, lesson_id)
);

-- Indexes
CREATE INDEX idx_progress_org_id ON lesson_progress (organization_id);
CREATE INDEX idx_progress_student ON lesson_progress (student_id);
CREATE INDEX idx_progress_lesson ON lesson_progress (lesson_id);
CREATE INDEX idx_progress_completed ON lesson_progress (completed);