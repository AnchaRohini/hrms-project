package com.mentis.hrms.service;

import com.mentis.hrms.model.Notification;
import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmployeeService employeeService;

    public Notification createNotification(String recipientId, String recipientType, String type, String title, String message, String referenceId, String referenceType, String sender) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setRecipientType(recipientType);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setSender(sender);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setReadAt(null); // Explicitly set as unread

        Notification saved = notificationRepository.save(notification);
        sendRealTimeNotification(saved);
        return saved;
    }

    public List<Notification> getNotifications(String recipientId, String recipientType) {
        return notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, recipientType);
    }

    public long getUnreadCount(String recipientId, String recipientType) {
        return notificationRepository.countByRecipientIdAndRecipientTypeAndReadAtIsNull(recipientId, recipientType);
    }

    public void markAsRead(Long notificationId, String recipientId, String recipientType) {
        notificationRepository.findByIdAndRecipientIdAndRecipientType(notificationId, recipientId, recipientType)
                .ifPresent(notification -> {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                });
    }

    public void markAllAsRead(String recipientId, String recipientType) {
        notificationRepository.markAllAsRead(recipientId, recipientType);
    }

    // UPDATED: Employee upload notification - ONLY to HR
    public void notifyDocumentUploaded(OnboardingDocument document) {
        Employee employee = document.getEmployee();

        // REMOVE employee notification for uploads
        // Only HR gets notification for employee uploads

        // HR notification:
        createNotification(
                "HR_SYSTEM",
                "HR",
                "DOCUMENT_UPLOADED",
                "📄 New Document Submitted",
                employee.getFirstName() + " " + employee.getLastName() +
                        " (Employee ID: " + employee.getEmployeeId() + ") uploaded: " + document.getDocumentName(),
                String.valueOf(document.getId()),
                "DOCUMENT",
                employee.getEmployeeId()
        );
    }

    // UPDATED: Enhanced document verification notification with proper message formatting
    public void notifyDocumentVerified(OnboardingDocument document, String verifiedBy) {
        Employee employee = document.getEmployee();
        String status = document.getStatus(); // This should be "VERIFIED" or "REJECTED"
        boolean isVerified = "VERIFIED".equals(status);

        String verificationNotes = document.getVerificationNotes() != null ?
                document.getVerificationNotes().replace("candidate", "employee") : "";

        String title = isVerified ? "✅ Document Verified" : "❌ Document Rejected";
        String message = isVerified ?
                String.format("✅ Your %s has been verified by %s.%s",
                        document.getDocumentName(), verifiedBy,
                        verificationNotes.isEmpty() ? "" : "\n\nHR Notes: " + verificationNotes) :
                String.format("❌ Your %s has been rejected by %s.%s",
                        document.getDocumentName(), verifiedBy,
                        verificationNotes.isEmpty() ? "" : "\n\nReason: " + verificationNotes);

        // Employee notification
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "DOCUMENT_" + status, // ✅ Creates DOCUMENT_VERIFIED or DOCUMENT_REJECTED
                title,
                message,
                String.valueOf(document.getId()),
                "DOCUMENT",
                verifiedBy
        );

        // HR notification
        createNotification(
                "HR_SYSTEM",
                "HR",
                "DOCUMENT_" + status,
                title,
                String.format("%s for %s %s has been %s by %s",
                        document.getDocumentName(), employee.getFirstName(),
                        employee.getLastName(), status.toLowerCase(), verifiedBy),
                String.valueOf(document.getId()),
                "DOCUMENT",
                verifiedBy
        );
    }

    public void notifyOnboardingCompleted(Employee employee) {
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "ONBOARDING_COMPLETED",
                "🎉 Onboarding Complete!",
                "Welcome to Menti's IT Solutions! Your onboarding process is complete.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM"
        );

        createNotification(
                "HR_SYSTEM",
                "HR",
                "ONBOARDING_COMPLETED",
                "🎉 Employee Onboarded",
                employee.getFirstName() + " " + employee.getLastName() + " (ID: " + employee.getEmployeeId() + ") has completed onboarding.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "SYSTEM"
        );
    }

    // ✅ ENHANCED: Notify employee when deadline is approaching
    public void notifyDeadlineApproaching(Employee employee, long hoursLeft) {
        String timeUnit = hoursLeft >= 24 ? (hoursLeft/24) + " days" : hoursLeft + " hours";
        String title = "⚠️ Document Upload Deadline Approaching";
        String message = "You have " + timeUnit + " left to upload all required documents. " +
                "Please complete your onboarding documents ASAP. Documents pending: " +
                (employee.getTotalDocuments() - employee.getSubmittedDocuments());

        System.out.println("📤 SENDING DEADLINE WARNING TO EMPLOYEE: " + employee.getEmployeeId());

        // Employee notification (THIS IS CRITICAL)
        createNotification(
                employee.getEmployeeId(),  // Recipient ID
                "EMPLOYEE",                 // Recipient type
                "DEADLINE_WARNING",
                title,
                message,
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM"
        );

        // HR notification (optional)
        createNotification(
                "HR_SYSTEM",
                "HR",
                "DEADLINE_WARNING",
                "Employee Deadline Approaching",
                employee.getFirstName() + " has " + timeUnit + " left to complete documents",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "SYSTEM"
        );
    }


    // ✅ ENHANCED: Notify employee when deadline is reached
    public void notifyDeadlineReached(Employee employee) {
        String title = "⏰ Document Upload Deadline Reached";
        String message = "Your document upload deadline has passed. " +
                "Please contact HR immediately to avoid delays. " +
                "Incomplete documents: " + (employee.getTotalDocuments() - employee.getSubmittedDocuments());

        System.out.println("🚨 SENDING DEADLINE OVERDUE TO EMPLOYEE: " + employee.getEmployeeId());

        // Employee notification (CRITICAL)
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "DEADLINE_REACHED",
                title,
                message,
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM"
        );

        // HR notification
        createNotification(
                "HR_SYSTEM",
                "HR",
                "DEADLINE_REACHED",
                "Employee Deadline Overdue",
                employee.getFirstName() + " (" + employee.getEmployeeId() + ") has missed their deadline",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "SYSTEM"
        );
    }

    // Notify employee when ALL documents are uploaded successfully
    public void notifyAllDocumentsUploaded(Employee employee) {
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "ALL_DOCUMENTS_UPLOADED",
                "✅ All Documents Uploaded Successfully!",
                "Congratulations! All your onboarding documents have been uploaded and submitted successfully. HR will now review them.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM"
        );
    }

    private String getNotificationColor(String type) {
        switch (type) {
            case "DOCUMENT_VERIFIED":
                return "success";
            case "DOCUMENT_REJECTED":
                return "danger";
            case "DOCUMENT_UPLOADED":
                return "info";
            case "ONBOARDING_COMPLETED":
                return "success";
            case "DEADLINE_WARNING":
                return "warning";
            case "DEADLINE_REACHED":
                return "danger";
            case "ALL_DOCUMENTS_UPLOADED":
                return "success";
            default:
                return "primary";
        }
    }

    private String getNotificationIcon(String type) {
        switch (type) {
            case "DOCUMENT_VERIFIED":
                return "fa-check-circle";
            case "DOCUMENT_REJECTED":
                return "fa-times-circle";
            case "DOCUMENT_UPLOADED":
                return "fa-file-upload";
            case "ONBOARDING_COMPLETED":
                return "fa-trophy";
            case "DEADLINE_WARNING":
                return "fa-clock";
            case "DEADLINE_REACHED":
                return "fa-exclamation-triangle";
            case "ALL_DOCUMENTS_UPLOADED":
                return "fa-check-double";
            default:
                return "fa-bell";
        }
    }

    private void sendRealTimeNotification(Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("type", notification.getType());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("referenceId", notification.getReferenceId());
        payload.put("sender", notification.getSender());
        payload.put("createdAt", notification.getCreatedAt().toString());
        payload.put("read", notification.isRead());

        // ✅ Ensure color/icon are set for deadline notifications
        payload.put("color", notification.getNotificationColor());
        payload.put("icon", notification.getNotificationIcon());

        String topic = String.format("/topic/%s/%s/notifications",
                notification.getRecipientType().toLowerCase(),
                notification.getRecipientId());

        System.out.println("🚀 Sending real-time notification to topic: " + topic);
        System.out.println("📦 Payload: " + payload);

        messagingTemplate.convertAndSend(topic, payload);
    }
}