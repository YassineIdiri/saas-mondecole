package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    Optional<Lesson> findByIdAndOrganizationId(Long id, Long organizationId);

    List<Lesson> findByOrganizationIdAndSectionIdOrderByOrderIndexAsc(Long organizationId, Long sectionId);
    long countByOrganizationIdAndSectionId(Long organizationId, Long sectionId);
    List<Lesson> findBySectionIdOrderByOrderIndexAsc(Long sectionId);

    int countBySectionId(Long sectionId);

    @Query("""
        SELECT l FROM Lesson l
        JOIN CourseSection s ON s.id = l.sectionId
        WHERE s.courseId = :courseId
          AND l.organizationId = :organizationId
        ORDER BY s.orderIndex ASC, l.orderIndex ASC
    """)
    List<Lesson> findAllByCourseId(
            @Param("organizationId") Long organizationId,
            @Param("courseId") Long courseId
    );
}