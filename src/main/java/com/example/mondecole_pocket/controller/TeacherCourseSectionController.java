package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.*;
import com.example.mondecole_pocket.security.CustomUserDetails;
import com.example.mondecole_pocket.service.TeacherCourseSectionService;
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
@RequestMapping("/api/teacher/courses/{courseId}/sections")
@RequiredArgsConstructor
public class TeacherCourseSectionController {

    private final TeacherCourseSectionService sectionService;

    /**
     * Get all sections for a course (with lesson count)
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<List<CourseSectionResponse>> getSections(
            @PathVariable Long courseId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<CourseSectionResponse> sections = sectionService.getSectionsByCourseId(
                courseId,
                currentUser.getId()
        );
        return ResponseEntity.ok(sections);
    }

    /**
     * Get section by ID (with lessons)
     */
    @GetMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseSectionDetailResponse> getSectionById(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CourseSectionDetailResponse section = sectionService.getSectionById(
                sectionId,
                courseId,
                currentUser.getId()
        );
        return ResponseEntity.ok(section);
    }

    /**
     * Create section
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseSectionResponse> createSection(
            @PathVariable Long courseId,
            @Valid @RequestBody CreateSectionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CourseSectionResponse section = sectionService.createSection(
                courseId,
                request,
                currentUser.getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(section);
    }

    /**
     * Update section
     */
    @PutMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<CourseSectionResponse> updateSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @Valid @RequestBody UpdateSectionRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        CourseSectionResponse section = sectionService.updateSection(
                sectionId,
                courseId,
                request,
                currentUser.getId()
        );
        return ResponseEntity.ok(section);
    }

    /**
     * Delete section (and all its lessons)
     */
    @DeleteMapping("/{sectionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> deleteSection(
            @PathVariable Long courseId,
            @PathVariable Long sectionId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        sectionService.deleteSection(sectionId, courseId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Reorder sections
     */
    @PatchMapping("/reorder")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<Void> reorderSections(
            @PathVariable Long courseId,
            @RequestBody List<ReorderRequest> requests,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        sectionService.reorderSections(courseId, requests, currentUser.getId());
        return ResponseEntity.ok().build();
    }
}