-- ════════════════════════════════════════════════════════
-- STEP 1 : Organisations supplémentaires
-- ════════════════════════════════════════════════════════

INSERT INTO organizations (
    name,
    slug,
    invitation_code,
    type,
    plan,
    max_users,
    max_storage_mb,
    active,
    created_at
)
SELECT
    'Organization Perf ' || i,
    'org-perf-' || i,
    upper(substring(md5(random()::text || i::text), 1, 12)),  -- ✅ Code unique
    (ARRAY['TRAINING_CENTER','UNIVERSITY','SCHOOL','COMPANY','OTHER'])[floor(random()*5+1)::int],
    'FREE',
    50,
    1000,
    true,
    NOW() - (random() * 365 || ' days')::interval
FROM generate_series(1, 11) AS i
ON CONFLICT (slug) DO NOTHING;

-- ════════════════════════════════════════════════════════
-- STEP 2 : 50,000 users
-- ════════════════════════════════════════════════════════

INSERT INTO users (
    organization_id,
    username,
    password_hash,
    email,
    first_name,
    last_name,
    role,
    active,
    locked,
    created_at
)
SELECT
    (random() * 9 + 1)::INT,
    'user_perf_' || i,
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.',
    'user_perf_' || i || '@test.com',
    'FirstName' || i,
    'LastName' || i,
    (ARRAY['STUDENT','STUDENT','STUDENT','TEACHER','ADMIN'])[floor(random()*5+1)::int],
    random() > 0.2,   -- 80% actifs
    random() > 0.9,   -- 10% verrouillés
    NOW() - (random() * 365 || ' days')::interval
FROM generate_series(1, 50000) AS i;

-- ════════════════════════════════════════════════════════
-- STEP 3 : 500 cours
-- ════════════════════════════════════════════════════════

INSERT INTO courses (
    organization_id,
    author_id,
    title,
    slug,
    summary,
    description,
    category,
    level,
    estimated_hours,
    language,
    published,
    published_at,
    active,
    created_at
)
SELECT
    u.organization_id,
    u.id,
    'Course ' || u.organization_id || '-' || row_number() OVER (PARTITION BY u.organization_id ORDER BY u.id),
    'course-perf-' || u.organization_id || '-' || u.id,
    'Summary for course by user ' || u.id,
    'Detailed description for course by user ' || u.id,
    (ARRAY['Programming','Design','Business','Marketing','Science'])[floor(random()*5+1)::int],
    (ARRAY['BEGINNER','INTERMEDIATE','ADVANCED'])[floor(random()*3+1)::int],
    (random() * 40 + 5)::INT,
    'fr',
    random() > 0.3,
    CASE WHEN random() > 0.3
         THEN NOW() - (random() * 180 || ' days')::interval
         ELSE NULL END,
    true,
    NOW() - (random() * 365 || ' days')::interval
FROM users u
WHERE u.role IN ('TEACHER', 'ADMIN')
LIMIT 500;

-- ════════════════════════════════════════════════════════
-- STEP 4 : Sections (5 par cours)
-- ════════════════════════════════════════════════════════

INSERT INTO course_sections (
    organization_id,
    course_id,
    title,
    description,
    order_index,
    created_at
)
SELECT
    c.organization_id,
    c.id,
    'Section ' || s.num || ' - Course ' || c.id,
    'Description section ' || s.num,
    s.num,
    NOW() - (random() * 300 || ' days')::interval
FROM courses c
CROSS JOIN generate_series(1, 5) AS s(num);

-- ════════════════════════════════════════════════════════
-- STEP 5 : Leçons (8 par section)
-- ════════════════════════════════════════════════════════

INSERT INTO lessons (
    organization_id,
    section_id,
    title,
    order_index,
    type,
    content,
    duration_seconds,
    downloadable,
    free,
    created_at
)
SELECT
    cs.organization_id,
    cs.id,
    'Lesson ' || l.num || ' - Section ' || cs.id,
    l.num,
    (ARRAY['VIDEO','TEXT','DOCUMENT','QUIZ'])[floor(random()*4+1)::int],
    'Content for lesson ' || l.num || ' section ' || cs.id,
    (random() * 1800 + 120)::INT,
    true,
    random() > 0.3,
    NOW() - (random() * 300 || ' days')::interval
FROM course_sections cs
CROSS JOIN generate_series(1, 8) AS l(num);

-- ════════════════════════════════════════════════════════
-- STEP 6 : 200,000 enrollments
-- ════════════════════════════════════════════════════════

INSERT INTO course_enrollments (
    organization_id,
    student_id,
    course_id,
    progress_percent,
    completed,
    completed_at,
    enrolled_at,
    last_accessed_at
)
SELECT DISTINCT ON (u.id, c.id)
    u.organization_id,
    u.id,
    c.id,
    (random() * 100)::INT,
    random() > 0.7,
    CASE WHEN random() > 0.7
         THEN NOW() - (random() * 90 || ' days')::interval
         ELSE NULL END,
    NOW() - (random() * 180 || ' days')::interval,
    CASE WHEN random() > 0.3
         THEN NOW() - (random() * 30 || ' days')::interval
         ELSE NULL END
FROM users u
JOIN courses c ON c.organization_id = u.organization_id AND c.published = true
WHERE u.role = 'STUDENT'
ORDER BY u.id, c.id, random()
LIMIT 200000;
