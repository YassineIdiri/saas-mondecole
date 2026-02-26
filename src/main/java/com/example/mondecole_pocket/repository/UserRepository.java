package com.example.mondecole_pocket.repository;

import com.example.mondecole_pocket.dto.UserStatsResponse;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.entity.Organization;
import com.example.mondecole_pocket.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.net.http.HttpHeaders;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // Multi-tenant queries
    boolean existsByUsernameAndOrganization(String username, Organization organization);

    boolean existsByEmailAndOrganization(String email, Organization organization);

    Optional<User> findByUsernameAndOrganization(String username, Organization organization);

    // ✅ AJOUT : Pour le login*
    Optional<User> findByUsernameAndOrganizationId(String username, Long organizationId);

    Optional<User> findByIdAndOrganization(Long id, Organization organization);

    // List users by organization
    Page<User> findByOrganization(Organization organization, Pageable pageable);

    Page<User> findByOrganizationAndRole(Organization organization, UserRole role, Pageable pageable);

    Page<User> findByOrganizationAndActive(Organization organization, boolean active, Pageable pageable);

    Page<User> findByOrganizationAndRoleAndActive(Organization organization, UserRole role, boolean active, Pageable pageable);

    // Count queries
    long countByOrganization(Organization organization);

    long countByOrganizationAndRole(Organization organization, UserRole role);

    long countByOrganizationAndActive(Organization organization, boolean active);
    long countByOrganizationAndRoleAndActive(Organization organization, UserRole role, boolean active);
    long countByOrganizationAndLocked(Organization organization, boolean locked);
    /**
     * ✅ NOUVEAU : Chercher user avec organization EAGER
     * Utilise un JOIN FETCH pour charger l'organization en une seule requête
     */
    @Query("SELECT u FROM User u JOIN FETCH u.organization WHERE u.username = :username")
    Optional<User> findByUsernameWithOrganization(String username);

    /**
     * ✅ NOUVEAU : Chercher user avec organization par organizationId
     */
    @Query("SELECT u FROM User u JOIN FETCH u.organization " +
            "WHERE u.username = :username AND u.organization.id = :organizationId")
    Optional<User> findByUsernameAndOrganizationIdWithOrganization(String username, Long organizationId);

    @Query("SELECT u.organization.id FROM User u WHERE u.username = :username")
    Optional<Long> findOrganizationIdByUsername(@Param("username") String username);


    Page<User> findByOrganizationIdAndRole(Long organizationId, UserRole role, Pageable pageable);

    @Query("""
      SELECT new com.example.mondecole_pocket.dto.UserStatsResponse(
          u.id,
          u.username,
          CONCAT(COALESCE(u.firstName, ''), ' ', COALESCE(u.lastName, '')),
          u.email,
          CAST(u.role AS string),
          CAST(COUNT(e.id) AS int)
      )
      FROM User u
      LEFT JOIN CourseEnrollment e ON e.studentId = u.id
      WHERE u.organization.id = :organizationId
      AND e.organizationId = :organizationId
      AND u.role = :role
      GROUP BY u.id, u.username, u.firstName, u.lastName, u.email, u.role
      ORDER BY u.createdAt DESC
    """)
        Page<UserStatsResponse> findUsersWithStats(
                Long organizationId,
                UserRole role,
                Pageable pageable
        );
}