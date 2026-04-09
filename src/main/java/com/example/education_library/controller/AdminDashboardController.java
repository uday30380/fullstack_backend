package com.example.education_library.controller;

import com.example.education_library.dto.AdminDashboardStatsDTO;
import com.example.education_library.dto.FeedbackDTO;
import com.example.education_library.dto.NotificationDTO;
import com.example.education_library.dto.ResourceDTO;
import com.example.education_library.dto.UserDTO;
import com.example.education_library.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin Dashboard", description = "Operations related to administrative overview and analytics")
public class AdminDashboardController {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceActionRepository actionRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/stats")
    @Operation(summary = "Get high-level repository analytics (Count overview)", operationId = "getDashboardStats")
    public AdminDashboardStatsDTO getStats() {
        System.out.println("TELEMETRY: Accessing Dashboard Statistics Registry...");
        return new AdminDashboardStatsDTO(
                resourceRepository.count(),
                userRepository.count(),
                actionRepository.countByActionType("DOWNLOAD"),
                feedbackRepository.count(),
                userRepository.countByRoleAndStatus("Faculty", "Pending")
        );
    }

    @GetMapping("/recent-uploads")
    @Operation(summary = "Get the latest 5 academic assets uploaded to the library", operationId = "getDashboardRecentUploads")
    public List<ResourceDTO> getRecentUploads() {
        System.out.println("TELEMETRY: Accessing Recent Upload Registry...");
        return resourceRepository.findTop5ByOrderByUploadDateDesc().stream()
                .map(r -> modelMapper.map(r, ResourceDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/recent-users")
    @Operation(summary = "Get the latest 5 scholastic identities registered", operationId = "getDashboardRecentUsers")
    public List<UserDTO> getRecentUsers() {
        System.out.println("TELEMETRY: Accessing Recent Identity Registry...");
        return userRepository.findTop5ByOrderByIdDesc().stream()
                .map(u -> modelMapper.map(u, UserDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/recent-feedback")
    @Operation(summary = "Get the latest 5 feedback entries submitted", operationId = "getDashboardRecentFeedback")
    public List<FeedbackDTO> getRecentFeedback() {
        return feedbackRepository.findTop5ByOrderByDateDesc().stream()
                .map(f -> modelMapper.map(f, FeedbackDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/recent-notifications")
    @Operation(summary = "Get all broadcasted notifications in the registry", operationId = "getDashboardRecentNotifications")
    public List<NotificationDTO> getRecentNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(n -> modelMapper.map(n, NotificationDTO.class))
                .collect(Collectors.toList());
    }
}
