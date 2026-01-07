package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "recipient_type", nullable = false)
    private String recipientType;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sender")
    private String sender;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    // NEW: Helper method to check if notification is read
    public boolean isRead() {
        return readAt != null;
    }

    // NEW: Helper method to mark as read
    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }

    // NEW: Helper method to mark as unread
    public void markAsUnread() {
        this.readAt = null;
    }

    // REMOVE or COMMENT OUT the getNotificationColor method from here
    // since we now have it in NotificationService
    /*
    // NEW: Get notification color based on type
    public String getNotificationColor() {
        switch (this.type) {
            case "DOCUMENT_UPLOADED":
                return "info"; // Blue
            case "DOCUMENT_VERIFIED":
                return "success"; // Green
            case "DOCUMENT_REJECTED":
                return "danger"; // Red
            case "ONBOARDING_COMPLETED":
                return "success"; // Green
            default:
                return "primary"; // Default purple
        }
    }
    */

    // REMOVE or COMMENT OUT the getNotificationIcon method from here
    /*
    // NEW: Get notification icon based on type
    public String getNotificationIcon() {
        switch (this.type) {
            case "DOCUMENT_UPLOADED":
                return "fa-file-upload";
            case "DOCUMENT_VERIFIED":
                return "fa-check-circle";
            case "DOCUMENT_REJECTED":
                return "fa-times-circle";
            case "ONBOARDING_COMPLETED":
                return "fa-trophy";
            default:
                return "fa-bell";
        }
    }
    */

    // NEW: Get notification color based on type
    public String getNotificationColor() {
        switch (this.type) {
            case "DOCUMENT_UPLOADED":
                return "info"; // Blue
            case "DOCUMENT_VERIFIED":
                return "success"; // Green
            case "DOCUMENT_REJECTED":
                return "danger"; // Red
            case "ONBOARDING_COMPLETED":
                return "success"; // Green

            case "DEADLINE_WARNING":
                return "warning"; // Orange
            case "DEADLINE_REACHED":
                return "danger"; // Red
            case "ALL_DOCUMENTS_UPLOADED":
                return "success"; // Green

            default:
                return "primary"; // Default purple
        }
    }

    // NEW: Get notification icon based on type
    public String getNotificationIcon() {
        switch (this.type) {
            case "DOCUMENT_UPLOADED":
                return "fa-file-upload";
            case "DOCUMENT_VERIFIED":
                return "fa-check-circle";
            case "DOCUMENT_REJECTED":
                return "fa-times-circle";
            case "ONBOARDING_COMPLETED":
                return "fa-trophy";
            default:
                return "fa-bell";

            case "DEADLINE_WARNING":
                return "fa-clock";
            case "DEADLINE_REACHED":
                return "fa-exclamation-triangle";
            case "ALL_DOCUMENTS_UPLOADED":
                return "fa-check-double";

        }
    }
}