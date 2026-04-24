package com.mentis.hrms.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_type", columnList = "announcement_type"),
        @Index(name = "idx_announcement_active", columnList = "is_active"),
        @Index(name = "idx_announcement_expiry", columnList = "expiry_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type;

    @Column(name = "announcement_type", length = 20)
    private String announcementType;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience")
    private TargetAudience targetAudience = TargetAudience.ALL;

    @Column(name = "created_by_name", length = 255)
    private String createdByName;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    // ===== ENUMS =====
    public enum AnnouncementType {
        PERMANENT, TEMPORARY
    }

    public enum AnnouncementPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum TargetAudience {
        ALL, HR_ONLY, EMPLOYEES_ONLY
    }

    // ===== LIFECYCLE =====
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isPinned == null) {
            this.isPinned = false;
        }
    }
}