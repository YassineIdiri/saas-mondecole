package com.example.mondecole_pocket.service;

import com.example.mondecole_pocket.dto.LessonContentResponse;
import com.example.mondecole_pocket.dto.UpdateLessonProgressRequest;
import com.example.mondecole_pocket.entity.*;
import com.example.mondecole_pocket.exception.CourseNotFoundException;
import com.example.mondecole_pocket.repository.*;
import com.example.mondecole_pocket.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonProgressRepository progressRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseSectionRepository sectionRepository;
    private final CourseRepository courseRepository;

    /**
     * Get lesson content with progress
     */
    @Transactional
    public LessonContentResponse getLessonContent(Long lessonId, Long studentId) {
        Long organizationId = TenantContext.getTenantId();

        // Get lesson
        Lesson lesson = lessonRepository.findByIdAndOrganizationId(lessonId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Lesson not found"));

        // Get section
        CourseSection section = sectionRepository.findById(lesson.getSectionId())
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        // Verify enrollment
        CourseEnrollment enrollment = enrollmentRepository
                .findByOrganizationIdAndStudentIdAndCourseId(organizationId, studentId, section.getCourseId())
                .orElseThrow(() -> new IllegalStateException("Not enrolled in this course"));

        // Update last accessed
        enrollment.setLastAccessedAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Get or create progress
        LessonProgress progress = progressRepository
                .findByOrganizationIdAndStudentIdAndLessonId(organizationId, studentId, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProgress = LessonProgress.builder()
                            .organizationId(organizationId)
                            .studentId(studentId)
                            .lessonId(lessonId)
                            .progressPercent(0)
                            .completed(false)
                            .viewCount(0)
                            .build();
                    return progressRepository.save(newProgress);
                });

        // Increment view count
        progress.setViewCount(progress.getViewCount() + 1);
        progress.setLastAccessedAt(LocalDateTime.now());
        progressRepository.save(progress);

        // Get navigation (previous/next lessons)
        List<Lesson> allLessons = lessonRepository.findAllByCourseId(organizationId, section.getCourseId());
        NavigationHelper nav = getNavigation(allLessons, lessonId);

        return toLessonContentResponse(lesson, section, progress, nav, allLessons.size());
    }

    /**
     * Update lesson progress
     */
    @Transactional
    public LessonContentResponse updateLessonProgress(
            Long lessonId,
            Long studentId,
            UpdateLessonProgressRequest request) {

        Long organizationId = TenantContext.getTenantId();

        // Get lesson
        Lesson lesson = lessonRepository.findByIdAndOrganizationId(lessonId, organizationId)
                .orElseThrow(() -> new CourseNotFoundException("Lesson not found"));

        // Get section
        CourseSection section = sectionRepository.findById(lesson.getSectionId())
                .orElseThrow(() -> new CourseNotFoundException("Section not found"));

        // Verify enrollment
        CourseEnrollment enrollment = enrollmentRepository
                .findByOrganizationIdAndStudentIdAndCourseId(organizationId, studentId, section.getCourseId())
                .orElseThrow(() -> new IllegalStateException("Not enrolled in this course"));

        // Get or create progress
        LessonProgress progress = progressRepository
                .findByOrganizationIdAndStudentIdAndLessonId(organizationId, studentId, lessonId)
                .orElseGet(() -> {
                    LessonProgress newProgress = LessonProgress.builder()
                            .organizationId(organizationId)
                            .studentId(studentId)
                            .lessonId(lessonId)
                            .progressPercent(0)
                            .completed(false)
                            .viewCount(1)
                            .build();
                    return progressRepository.save(newProgress);
                });

        // Update progress
        if (request.progressPercent() != null) {
            progress.setProgressPercent(request.progressPercent());
        }

        if (request.lastPositionSeconds() != null) {
            progress.setLastPositionSeconds(request.lastPositionSeconds());
        }

        if (request.completed() != null && request.completed() && !progress.getCompleted()) {
            progress.setCompleted(true);
            progress.setCompletedAt(LocalDateTime.now());
            progress.setProgressPercent(100);

            log.info("✅ Lesson {} completed by student {}", lessonId, studentId);
        }

        progress.setLastAccessedAt(LocalDateTime.now());
        progress = progressRepository.save(progress);

        // Update course enrollment progress
        updateCourseProgress(enrollment, section.getCourseId(), studentId, organizationId);

        // Get navigation
        List<Lesson> allLessons = lessonRepository.findAllByCourseId(organizationId, section.getCourseId());
        NavigationHelper nav = getNavigation(allLessons, lessonId);

        return toLessonContentResponse(lesson, section, progress, nav, allLessons.size());
    }

    /**
     * Mark lesson as completed
     */
    @Transactional
    public void markLessonCompleted(Long lessonId, Long studentId) {
        UpdateLessonProgressRequest request = new UpdateLessonProgressRequest(100, null, true);
        updateLessonProgress(lessonId, studentId, request);
    }

    // ════════════════════════════════════════════════════════
    // HELPERS
    // ════════════════════════════════════════════════════════

    private void updateCourseProgress(CourseEnrollment enrollment, Long courseId, Long studentId, Long organizationId) {
        // Get all lessons in course
        List<Lesson> allLessons = lessonRepository.findAllByCourseId(organizationId, courseId);

        if (allLessons.isEmpty()) {
            return;
        }

        // Get lesson IDs
        List<Long> lessonIds = allLessons.stream().map(Lesson::getId).toList();

        // Count completed lessons
        long completedCount = progressRepository.countByOrganizationIdAndStudentIdAndLessonIdInAndCompletedTrue(
                organizationId, studentId, lessonIds);

        // Calculate progress
        int progressPercent = (int) ((completedCount * 100) / allLessons.size());
        enrollment.setProgressPercent(progressPercent);

        // Check if course is completed
        if (progressPercent == 100 && !enrollment.getCompleted()) {
            enrollment.setCompleted(true);
            enrollment.setCompletedAt(LocalDateTime.now());
            log.info("🎉 Course {} completed by student {}", courseId, studentId);
        }

        enrollmentRepository.save(enrollment);
    }

    private NavigationHelper getNavigation(List<Lesson> allLessons, Long currentLessonId) {
        int currentIndex = -1;
        for (int i = 0; i < allLessons.size(); i++) {
            if (allLessons.get(i).getId().equals(currentLessonId)) {
                currentIndex = i;
                break;
            }
        }

        Long previousId = null;
        Long nextId = null;

        if (currentIndex > 0) {
            previousId = allLessons.get(currentIndex - 1).getId();
        }

        if (currentIndex >= 0 && currentIndex < allLessons.size() - 1) {
            nextId = allLessons.get(currentIndex + 1).getId();
        }

        return new NavigationHelper(previousId, nextId, currentIndex + 1);
    }

    private LessonContentResponse toLessonContentResponse(
            Lesson lesson,
            CourseSection section,
            LessonProgress progress,
            NavigationHelper nav,
            int totalLessons) {

        LessonContentResponse.ProgressInfo progressInfo = new LessonContentResponse.ProgressInfo(
                progress.getCompleted(),
                progress.getProgressPercent(),
                progress.getLastPositionSeconds()
        );

        LessonContentResponse.NavigationInfo navigationInfo = new LessonContentResponse.NavigationInfo(
                nav.previousLessonId(),
                nav.nextLessonId(),
                section.getTitle(),
                nav.lessonNumber(),
                totalLessons
        );

        return new LessonContentResponse(
                lesson.getId(),
                lesson.getTitle(),
                lesson.getType().name(),
                lesson.getDescription(),
                lesson.getContent(),
                lesson.getFileUrl(),
                lesson.getFileName(),
                lesson.getMimeType(),
                lesson.getFileSizeBytes(),
                lesson.getDurationSeconds(),
                lesson.getExternalVideoUrl(),
                lesson.getDownloadable(),
                progressInfo,
                navigationInfo
        );
    }

    private record NavigationHelper(
            Long previousLessonId,
            Long nextLessonId,
            int lessonNumber
    ) {}
}