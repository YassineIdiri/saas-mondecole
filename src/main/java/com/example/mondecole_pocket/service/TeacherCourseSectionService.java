package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.entity.*;
import com.example.mondecole_pocket.exception.CourseNotFoundException;
import com.example.mondecole_pocket.repository.*;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherCourseSectionService {

    private final CourseSectionRepository sectionRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    /**
     * Get all sections for a course
     */
    @Transactional(readOnly = true)
    public List<CourseSectionResponse> getSectionsByCourseId(Long courseId, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify course belongs to teacher
        courseRepository.findByIdAndOrganizationIdAndAuthorId(courseId, organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        List<CourseSection> sections = sectionRepository.findByCourseIdOrderByOrderIndexAsc(courseId);

        return sections.stream()
                .map(section -> {
                    int lessonCount = lessonRepository.countBySectionId(section.getId());
                    return toCourseSectionResponse(section, lessonCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get section by ID (with lessons)
     */
    @Transactional(readOnly = true)
    public CourseSectionDetailResponse getSectionById(Long sectionId, Long courseId, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify course ownership
        courseRepository.findByIdAndOrganizationIdAndAuthorId(courseId, organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        CourseSection section = sectionRepository.findByIdAndOrganizationId(sectionId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        if (!section.getCourseId().equals(courseId)) {
            throw new IllegalStateException("Section does not belong to this course");
        }

        // Get lessons
        List<Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);

        return toCourseSectionDetailResponse(section, lessons);
    }

    /**
     * Create section
     */
    @Transactional
    public CourseSectionResponse createSection(Long courseId, CreateSectionRequest request, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify course ownership
        courseRepository.findByIdAndOrganizationIdAndAuthorId(courseId, organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        CourseSection section = CourseSection.builder()
                .organizationId(organizationId)
                .courseId(courseId)
                .title(request.title())
                .description(request.description())
                .orderIndex(request.orderIndex())
                .build();

        section = sectionRepository.save(section);

        log.info("✅ Section created: id={}, title={}, course={}",
                section.getId(), section.getTitle(), courseId);

        return toCourseSectionResponse(section, 0);
    }

    /**
     * Update section
     */
    @Transactional
    public CourseSectionResponse updateSection(
            Long sectionId,
            Long courseId,
            UpdateSectionRequest request,
            Long teacherId) {

        Long organizationId = TenantContext.getTenantId();

        // Verify course ownership
        courseRepository.findByIdAndOrganizationIdAndAuthorId(courseId, organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        CourseSection section = sectionRepository.findByIdAndOrganizationId(sectionId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        if (!section.getCourseId().equals(courseId)) {
            throw new IllegalStateException("Section does not belong to this course");
        }

        // Update fields
        if (request.title() != null) {
            section.setTitle(request.title());
        }
        if (request.description() != null) {
            section.setDescription(request.description());
        }
        if (request.orderIndex() != null) {
            section.setOrderIndex(request.orderIndex());
        }

        section = sectionRepository.save(section);

        log.info("✅ Section updated: id={}, title={}", section.getId(), section.getTitle());

        int lessonCount = lessonRepository.countBySectionId(section.getId());
        return toCourseSectionResponse(section, lessonCount);
    }

    /**
     * Delete section (and all its lessons CASCADE)
     */
    @Transactional
    public void deleteSection(Long sectionId, Long courseId, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify course ownership
        courseRepository.findByIdAndOrganizationIdAndAuthorId(courseId, organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        CourseSection section = sectionRepository.findByIdAndOrganizationId(sectionId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        if (!section.getCourseId().equals(courseId)) {
            throw new IllegalStateException("Section does not belong to this course");
        }

        // Delete all lessons first (if not CASCADE)
        List<Lesson> lessons = lessonRepository.findBySectionIdOrderByOrderIndexAsc(sectionId);
        lessonRepository.deleteAll(lessons);

        // Delete section
        sectionRepository.delete(section);

        log.info("✅ Section deleted: id={}, title={}, lessons={}",
                section.getId(), section.getTitle(), lessons.size());
    }

    /**
     * Reorder sections
     */
    @Transactional
    public void reorderSections(Long courseId, List<ReorderRequest> requests, Long teacherId) {
        Long organizationId = TenantContext.getTenantId();

        // Verify course ownership
        courseRepository.findByIdAndOrganizationIdAndAuthorId(courseId, organizationId, teacherId)
                .orElseThrow(() -> new CourseNotFoundException("Course not found or unauthorized"));

        for (ReorderRequest request : requests) {
            CourseSection section = sectionRepository.findById(request.id())
                    .orElseThrow(() -> new CourseNotFoundException("Section not found: " + request.id()));

            if (!section.getCourseId().equals(courseId)) {
                throw new IllegalStateException("Section does not belong to this course");
            }

            section.setOrderIndex(request.orderIndex());
            sectionRepository.save(section);
        }

        log.info("✅ Sections reordered in course {}", courseId);
    }

    // ════════════════════════════════════════════════════════
    // MAPPERS
    // ════════════════════════════════════════════════════════

    private CourseSectionResponse toCourseSectionResponse(CourseSection section, int lessonCount) {
        return new CourseSectionResponse(
                section.getId(),
                section.getCourseId(),
                section.getTitle(),
                section.getDescription(),
                section.getOrderIndex(),
                lessonCount
        );
    }

    private CourseSectionDetailResponse toCourseSectionDetailResponse(
            CourseSection section,
            List<Lesson> lessons) {

        List<CourseSectionDetailResponse.LessonSummary> lessonSummaries = lessons.stream()
                .map(lesson -> new CourseSectionDetailResponse.LessonSummary(
                        lesson.getId(),
                        lesson.getTitle(),
                        lesson.getType().name(),
                        lesson.getOrderIndex(),
                        lesson.getDurationSeconds()
                ))
                .collect(Collectors.toList());

        return new CourseSectionDetailResponse(
                section.getId(),
                section.getCourseId(),
                section.getTitle(),
                section.getDescription(),
                section.getOrderIndex(),
                lessonSummaries
        );
    }
}