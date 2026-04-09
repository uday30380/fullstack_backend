package com.example.education_library.repository;

import com.example.education_library.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByTargetRoleInOrderByCreatedAtDesc(Collection<String> roles);
    List<Notification> findAllByOrderByCreatedAtDesc();
}
