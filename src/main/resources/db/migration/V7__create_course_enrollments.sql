CREATE TABLE course_enrollments (
    id BIGSERIAL PRIMARY KEY,

    -- Multi-tenant
    organization_id BIGINT NOT NULL,

    -- Relations
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,

    -- Progress
    progress_percent INTEGER NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMP,

    -- Evaluation
    final_grade DOUBLE PRECISION,

    -- Certificate
    certificate_issued BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_url VARCHAR(500),

    -- Timestamps
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP,
    updated_at TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_enrollment_organization FOREIGN KEY (organization_id)
        REFERENCES organizations (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_student FOREIGN KEY (student_id)
        REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_enrollment_course FOREIGN KEY (course_id)
        REFERENCES courses (id) ON DELETE CASCADE,

    -- Unique constraint
    CONSTRAINT uk_enrollment_student_course UNIQUE (student_id, course_id)
);

-- Indexes
CREATE INDEX idx_enrollment_org_id ON course_enrollments (organization_id);
CREATE INDEX idx_enrollment_student ON course_enrollments (student_id);
CREATE INDEX idx_enrollment_course ON course_enrollments (course_id);
CREATE INDEX idx_enrollment_completed ON course_enrollments (completed);