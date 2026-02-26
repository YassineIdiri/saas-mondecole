package com.example.mondecole_pocket.dto;

import java.util.List;

public record CourseSectionDetailResponse(
        Long id,
        Long courseId,
        String title,
        String description,
        Integer orderIndex,
        List<LessonSummary> lessons
) {
    public record LessonSummary(
            Long id,
            String title,
            String type,
            Integer orderIndex,
            Integer durationSeconds
    ) {}
}