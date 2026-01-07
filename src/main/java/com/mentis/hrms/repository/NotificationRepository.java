package com.mentis.hrms.repository;

import com.mentis.hrms.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(String recipientId, String recipientType);

    // UPDATED: Use readAt IS NULL instead of readFalse
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.recipientId = ?1 AND n.recipientType = ?2 AND n.readAt IS NULL")
    long countByRecipientIdAndRecipientTypeAndReadAtIsNull(String recipientId, String recipientType);

    Optional<Notification> findByIdAndRecipientIdAndRecipientType(Long id, String recipientId, String recipientType);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.recipientId = :recipientId AND n.recipientType = :recipientType AND n.readAt IS NULL")
    void markAllAsRead(@Param("recipientId") String recipientId, @Param("recipientType") String recipientType);
}