package com.mentis.hrms.dto;

import com.mentis.hrms.model.Announcement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementDTO {

    private Long id;
    private String title;
    private String description;
    private String type;
    private String announcementType;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private String category;
    private String priority;
    private String targetAudience;
    private String createdByName;
    private LocalDateTime expiresAt;
    private Boolean isPinned;

    // ===== MAPPER: Entity → DTO =====
    public static AnnouncementDTO fromEntity(Announcement announcement) {
        if (announcement == null) {
            return null;
        }

        AnnouncementDTO dto = new AnnouncementDTO();
        dto.setId(announcement.getId());
        dto.setTitle(announcement.getTitle());
        dto.setDescription(announcement.getDescription());
        dto.setType(announcement.getType() != null ? announcement.getType().toString() : null);
        dto.setAnnouncementType(announcement.getAnnouncementType());
        dto.setCreatedBy(announcement.getCreatedBy());
        dto.setCreatedAt(announcement.getCreatedAt());
        dto.setExpiryDate(announcement.getExpiryDate());
        dto.setIsActive(announcement.getIsActive() != null ? announcement.getIsActive() : true);
        dto.setCategory(announcement.getCategory());
        dto.setPriority(announcement.getPriority() != null ? announcement.getPriority().toString() : "NORMAL");
        dto.setTargetAudience(announcement.getTargetAudience() != null ? announcement.getTargetAudience().toString() : "ALL");
        dto.setCreatedByName(announcement.getCreatedByName());
        dto.setExpiresAt(announcement.getExpiresAt());
        dto.setIsPinned(announcement.getIsPinned() != null ? announcement.getIsPinned() : false);

        return dto;
    }

    // ===== MAPPER: DTO → Entity =====
    public Announcement toEntity() {
        Announcement announcement = new Announcement();
        announcement.setId(this.id);
        announcement.setTitle(this.title);
        announcement.setDescription(this.description);

        if (this.type != null) {
            announcement.setType(Announcement.AnnouncementType.valueOf(this.type));
        }

        announcement.setAnnouncementType(this.announcementType);
        announcement.setCreatedBy(this.createdBy);
        announcement.setCreatedAt(this.createdAt);
        announcement.setExpiryDate(this.expiryDate);
        announcement.setIsActive(this.isActive != null ? this.isActive : true);
        announcement.setCategory(this.category);

        if (this.priority != null) {
            announcement.setPriority(Announcement.AnnouncementPriority.valueOf(this.priority));
        } else {
            announcement.setPriority(Announcement.AnnouncementPriority.NORMAL);
        }

        if (this.targetAudience != null) {
            announcement.setTargetAudience(Announcement.TargetAudience.valueOf(this.targetAudience));
        } else {
            announcement.setTargetAudience(Announcement.TargetAudience.ALL);
        }

        announcement.setCreatedByName(this.createdByName);
        announcement.setExpiresAt(this.expiresAt);
        announcement.setIsPinned(this.isPinned != null ? this.isPinned : false);

        return announcement;
    }

    // ===== HELPER METHODS (For backward compatibility) =====
    public String getContent() {
        return this.description;
    }

    public void setContent(String content) {
        this.description = content;
    }

    public String getStatus() {
        return this.isActive ? "ACTIVE" : "INACTIVE";
    }

    public String getFormattedDate() {
        if (this.createdAt == null) return "Not set";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return this.createdAt.format(formatter);
    }

    public String getFormattedExpiryDate() {
        if (this.expiresAt == null) return "No expiry";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return this.expiresAt.format(formatter);
    }
}