package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.entity.LessonProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    Optional<LessonProgress> findByOrganizationIdAndStudentIdAndLessonId(Long organizationId, Long studentId, Long lessonId);

    List<LessonProgress> findByOrganizationIdAndStudentId(Long organizationId, Long studentId);

    @Query("SELECT lp FROM LessonProgress lp WHERE lp.organizationId = ?1 AND lp.studentId = ?2 " +
            "AND lp.lessonId IN (SELECT l.id FROM Lesson l WHERE l.sectionId IN " +
            "(SELECT s.id FROM CourseSection s WHERE s.courseId = ?3))")
    List<LessonProgress> findByStudentAndCourse(Long organizationId, Long studentId, Long courseId);

    long countByOrganizationIdAndStudentIdAndLessonIdInAndCompletedTrue(Long organizationId, Long studentId, List<Long> lessonIds);
}