package com.example.mondecole_pocket.entity;
import com.example.mondecole_pocket.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users"
      /* ,indexes = {
                @Index(name = "idx_user_username", columnList = "username"),
                @Index(name = "idx_user_org_id", columnList = "organization_id"),
                @Index(name = "idx_user_org_username", columnList = "organization_id, username", unique = true)
        }*/
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ════════════════════════════════════════════════════════
     * MULTI-TENANT : Lien vers l'organisation
     * ════════════════════════════════════════════════════════
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Username unique PAR ORGANISATION
     * Exemple : "john" peut exister dans org1 ET org2
     */
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    /**
     * Email (optionnel, peut être null pour certains users)
     */
    @Column(length = 100)
    private String email;

    @Column(length = 100)
    private String firstName;

    @Column(length = 100)
    private String lastName;

    /**
     * Rôle DANS cette organisation
     * Exemples : ADMIN, TEACHER, STUDENT
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private boolean locked = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastLoginAt;

    private LocalDateTime updatedAt;

    public String getFullName() {
        if (firstName == null && lastName == null) return username;
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    public boolean belongsToOrganization(Long orgId) {
        return organization != null && organization.getId().equals(orgId);
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}