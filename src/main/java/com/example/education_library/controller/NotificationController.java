package com.example.education_library.controller;

import com.example.education_library.dto.NotificationDTO;
import com.example.education_library.model.Notification;
import com.example.education_library.repository.NotificationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notification Management", description = "Operations related to administrative broadcasts and alerts")
public class NotificationController {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get all broadcasted notifications in the registry", operationId = "getAllNotifications")
    public List<NotificationDTO> getAllNotifications() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(n -> modelMapper.map(n, NotificationDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/role/{role}")
    @Operation(summary = "Get notifications targeted at a specific scholastic role or 'All'", operationId = "getNotificationsByRole")
    public List<NotificationDTO> getNotificationsForRole(@PathVariable String role) {
        List<Notification> notifications;
        if (role.equalsIgnoreCase("Admin")) {
            notifications = repository.findAllByOrderByCreatedAtDesc();
        } else {
            notifications = repository.findByTargetRoleInOrderByCreatedAtDesc(Arrays.asList(role, "All"));
        }
        return notifications.stream()
                .map(n -> modelMapper.map(n, NotificationDTO.class))
                .collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "Establish a new administrative broadcast notification", operationId = "createNotification")
    public NotificationDTO createNotification(@RequestBody NotificationDTO notificationDTO) {
        Notification notification = modelMapper.map(notificationDTO, Notification.class);
        Notification saved = repository.save(notification);
        return modelMapper.map(saved, NotificationDTO.class);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modify an existing broadcast notification metadata", operationId = "updateNotification")
    public ResponseEntity<NotificationDTO> updateNotification(@PathVariable Long id, @RequestBody NotificationDTO notificationDTO) {
        return repository.findById(id)
                .map(n -> {
                    n.setTitle(notificationDTO.getTitle());
                    n.setMessage(notificationDTO.getMessage());
                    n.setType(notificationDTO.getType());
                    n.setTargetRole(notificationDTO.getTargetRole());
                    Notification saved = repository.save(n);
                    return ResponseEntity.ok(modelMapper.map(saved, NotificationDTO.class));
                }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Terminate a broadcast notification from the registry", operationId = "deleteNotification")
    public void deleteNotification(@PathVariable Long id) {
        repository.deleteById(id);
    }
}
