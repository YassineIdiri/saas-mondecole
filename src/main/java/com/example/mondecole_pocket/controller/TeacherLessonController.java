package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.security.CustomUserDetails;
import com.example.mondecole_pocket.service.TeacherLessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/teacher/sections/{sectionId}/lessons")  // ✅ Route pour profs
@RequiredArgsConstructor
public class TeacherLessonController {

    private final TeacherLessonService teacherLessonService;

    /**
     * Get all lessons in a section (for editing)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<LessonListResponse>> getLessons(
            @PathVariable Long sectionId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<LessonListResponse> lessons = teacherLessonService.getLessonsBySectionId(
                sectionId,
                currentUser.getId()
        );
        return ResponseEntity.ok(lessons);
    }

    /**
     * Get lesson by ID (for editing)
     */
    @GetMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDetailResponse> getLessonById(
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LessonDetailResponse lesson = teacherLessonService.getLessonById(
                lessonId,
                sectionId,
                currentUser.getId()
        );
        return ResponseEntity.ok(lesson);
    }

    /**
     * Create lesson (Teacher writes the text content here!)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDetailResponse> createLesson(
            @PathVariable Long sectionId,
            @Valid @RequestBody CreateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LessonDetailResponse lesson = teacherLessonService.createLesson(
                sectionId,
                request,
                currentUser.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(lesson);
    }

    /**
     * Update lesson (Teacher edits the content)
     */
    @PutMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<LessonDetailResponse> updateLesson(
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @Valid @RequestBody UpdateLessonRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        LessonDetailResponse lesson = teacherLessonService.updateLesson(
                lessonId,
                sectionId,
                request,
                currentUser.getId()
        );
        return ResponseEntity.ok(lesson);
    }

    /**
     * Delete lesson
     */
    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable Long sectionId,
            @PathVariable Long lessonId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        teacherLessonService.deleteLesson(lessonId, sectionId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorder lessons
     */
    @PatchMapping("/reorder")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> reorderLessons(
            @PathVariable Long sectionId,
            @RequestBody List<ReorderRequest> requests,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        teacherLessonService.reorderLessons(sectionId, requests, currentUser.getId());
        return ResponseEntity.ok().build();
    }
}