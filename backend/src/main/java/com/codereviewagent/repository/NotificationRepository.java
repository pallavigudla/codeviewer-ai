package com.codereviewagent.repository;

import com.codereviewagent.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserId(UUID userId);
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Notification> findByUserIdAndIsReadFalse(UUID userId);
    long countByUserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId")
    int markAllAsReadByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id IN :ids AND n.user.id = :userId")
    int markAsReadByIdsAndUserId(@Param("ids") List<UUID> ids, @Param("userId") UUID userId);
}
