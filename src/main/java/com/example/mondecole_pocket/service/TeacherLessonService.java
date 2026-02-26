package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.*;
import com.example.mondecole_pocket.entity.enums.LessonType;
import com.example.mondecole_pocket.exception.CourseNotFoundException;
import com.example.mondecole_pocket.repository.*;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherLessonService {

    private final LessonRepository lessonRepository;
    private final CourseSectionRepository sectionRepository;
    private final CourseRepository courseRepository;

    /**
     * Get all lessons in a section
     */
    @Transactional(readOnly = true)
    public List<LessonListResponse> getLessonsBySectionId(Long sectionId, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify section belongs to teacher's course
        CourseSection section = sectionRepository.findByIdAndOrganizationId(sectionId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        Course course = courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        section.getCourseId(), organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        List<Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);

        return lessons.stream()
                .map(this::toLessonListResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get lesson by ID (for editing)
     */
    @Transactional(readOnly = true)
    public LessonDetailResponse getLessonById(Long lessonId, Long sectionId, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        Lesson lesson = lessonRepository.findByIdAndOrganizationId(lessonId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Lesson not found"));

        if (!lesson.getSectionId().equals(sectionId)) {
            throw new IllegalStateException("Lesson does not belong to this section");
        }

        // Verify ownership
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        section.getCourseId(), organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Unauthorized"));

        return toLessonDetailResponse(lesson);
    }

    /**
     * Create lesson (Teacher writes content here!)
     */
    @Transactional
    public LessonDetailResponse createLesson(Long sectionId, CreateLessonRequest request, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify section and ownership
        CourseSection section = sectionRepository.findByIdAndOrganizationId(sectionId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        section.getCourseId(), organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Unauthorized"));

        Lesson lesson = Lesson.builder()
                .organizationId(organizationId)
                .sectionId(sectionId)
                .title(request.title())
                .description(request.description())
                .type(LessonType.valueOf(request.type()))
                .content(request.content())  // ✅ Le texte écrit par le prof !
                .orderIndex(request.orderIndex())
                .externalVideoUrl(request.externalVideoUrl())
                .durationSeconds(request.durationSeconds())
                .downloadable(request.downloadable() != null ? request.downloadable() : false)
                .build();

        lesson = lessonRepository.save(lesson);

        log.info("✅ Lesson created: id={}, title={}, section={}",
                lesson.getId(), lesson.getTitle(), sectionId);

        return toLessonDetailResponse(lesson);
    }

    /**
     * Update lesson
     */
    @Transactional
    public LessonDetailResponse updateLesson(
            Long lessonId,
            Long sectionId,
            UpdateLessonRequest request,
            Long teacherId) {

        Long organizationId = TenantContext.getTenantId();

        Lesson lesson = lessonRepository.findByIdAndOrganizationId(lessonId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Lesson not found"));

        // Verify ownership
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        section.getCourseId(), organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Unauthorized"));

        // Update fields
        if (request.title() != null) {
            lesson.setTitle(request.title());
        }
        if (request.description() != null) {
            lesson.setDescription(request.description());
        }
        if (request.content() != null) {
            lesson.setContent(request.content());  // ✅ Update text content
        }
        if (request.type() != null) {
            lesson.setType(LessonType.valueOf(request.type()));
        }
        if (request.orderIndex() != null) {
            lesson.setOrderIndex(request.orderIndex());
        }
        if (request.externalVideoUrl() != null) {
            lesson.setExternalVideoUrl(request.externalVideoUrl());
        }
        if (request.durationSeconds() != null) {
            lesson.setDurationSeconds(request.durationSeconds());
        }
        if (request.downloadable() != null) {
            lesson.setDownloadable(request.downloadable());
        }

        lesson = lessonRepository.save(lesson);

        log.info("✅ Lesson updated: id={}, title={}", lesson.getId(), lesson.getTitle());

        return toLessonDetailResponse(lesson);
    }

    /**
     * Delete lesson
     */
    @Transactional
    public void deleteLesson(Long lessonId, Long sectionId, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        Lesson lesson = lessonRepository.findByIdAndOrganizationId(lessonId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Lesson not found"));

        // Verify ownership
        CourseSection section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        section.getCourseId(), organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Unauthorized"));

        lessonRepository.delete(lesson);

        log.info("✅ Lesson deleted: id={}, title={}", lesson.getId(), lesson.getTitle());
    }

    /**
     * Reorder lessons
     */
    @Transactional
    public void reorderLessons(Long sectionId, List<ReorderRequest> requests, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify ownership
        CourseSection section = sectionRepository.findByIdAndOrganizationId(sectionId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        courseRepository.findByIdAndOrganizationIdAndAuthorId(
                        section.getCourseId(), organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Unauthorized"));

        for (ReorderRequest request : requests) {
            Lesson lesson = lessonRepository.findById(request.id())
                    .orElseThrow(() -> new CourseNotFoundException("Lesson not found: " + request.id()));

            if (!lesson.getSectionId().equals(sectionId)) {
                throw new IllegalStateException("Lesson does not belong to this section");
            }

            lesson.setOrderIndex(request.orderIndex());
            lessonRepository.save(lesson);
        }

        log.info("✅ Lessons reordered in section {}", sectionId);
    }

    // ════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════

    private LessonListResponse toLessonListResponse(Lesson lesson) {
        return new LessonListResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getType().name(),
                lesson.getOrderIndex(),
                lesson.getDurationSeconds()
        );
    }

    private LessonDetailResponse toLessonDetailResponse(Lesson lesson) {
        return new LessonDetailResponse(
                lesson.getId(),
                lesson.getSectionId(),
                lesson.getTitle(),
                lesson.getDescription(),
                lesson.getContent(),  // ✅ Le contenu texte complet
                lesson.getType().name(),
                lesson.getOrderIndex(),
                lesson.getExternalVideoUrl(),
                lesson.getDurationSeconds(),
                lesson.getDownloadable()
        );
    }
}