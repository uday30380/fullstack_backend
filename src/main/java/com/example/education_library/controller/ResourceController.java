package com.example.education_library.controller;

import com.example.education_library.dto.ResourceActionDTO;
import com.example.education_library.dto.ResourceDTO;
import com.example.education_library.model.ResourceAction;
import com.example.education_library.repository.ResourceActionRepository;
import com.example.education_library.repository.ResourceRepository;
import com.example.education_library.repository.UserRepository;
import com.example.education_library.service.EmailService;
import com.example.education_library.service.FileStorageService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/resources")
@Tag(name = "Resource Management", description = "Operations related to academic resources")
public class ResourceController {

    @Autowired
    private ResourceActionRepository actionRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get all accessible resources based on role and node", operationId = "getAllResources")
    public List<ResourceDTO> getAllResources(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String joinedPin
    ) {
        String activeRole = (role == null || role.trim().isEmpty()) ? "guest" : role;
        List<com.example.education_library.model.Resource> resources;
        
        if ("Student".equalsIgnoreCase(activeRole) || "Faculty".equalsIgnoreCase(activeRole)) {
            if (joinedPin != null && !joinedPin.trim().isEmpty() && !"undefined".equals(joinedPin)) {
                resources = resourceRepository.findActiveResourcesByFacultyPin(joinedPin);
            } else {
                resources = resourceRepository.findOfficialAdminResources();
            }
        } else if ("Admin".equalsIgnoreCase(activeRole)) {
            resources = resourceRepository.findActiveResources();
        } else {
            resources = resourceRepository.findOfficialAdminResources();
        }

        return resources.stream()
                .map(r -> modelMapper.map(r, ResourceDTO.class))
                .collect(Collectors.toList());
    }
    
    @GetMapping("/featured")
    @Operation(summary = "Get featured resources", operationId = "getFeaturedResources")
    public List<ResourceDTO> getFeatured(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String joinedPin
    ) {
        List<com.example.education_library.model.Resource> resources;
        if ("Student".equalsIgnoreCase(role) || "Faculty".equalsIgnoreCase(role)) {
             if (joinedPin != null && !joinedPin.trim().isEmpty() && !"undefined".equals(joinedPin)) {
                 resources = resourceRepository.findFeaturedByFacultyPin(joinedPin);
             } else {
                 resources = resourceRepository.findFeaturedOfficialAdminResources();
             }
        } else if ("guest".equalsIgnoreCase(role) || role == null) {
            resources = resourceRepository.findFeaturedOfficialAdminResources();
        } else {
            resources = resourceRepository.findTop3ByIsFeaturedTrueOrderByFeaturedOrderAsc();
        }

        return resources.stream()
                .map(r -> modelMapper.map(r, ResourceDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resource details by ID", operationId = "getResourceById")
    public ResourceDTO getResourceById(@PathVariable Long id) {
        return resourceRepository.findById(id)
                .map(r -> modelMapper.map(r, ResourceDTO.class))
                .orElse(null);
    }

    @GetMapping("/{userId}/actions")
    @Operation(summary = "Get history of resource actions for a user", operationId = "getUserActions")
    public List<ResourceActionDTO> getActions(@PathVariable Long userId) {
        return actionRepository.findByUserIdOrderByActionDateDesc(userId).stream()
                .map(a -> modelMapper.map(a, ResourceActionDTO.class))
                .collect(Collectors.toList());
    }

    @PostMapping("/action")
    @Operation(summary = "Log an action (Bookmark, Download, Save) on a resource", operationId = "addResourceAction")
    public ResponseEntity<ResourceActionDTO> addAction(@RequestBody ResourceActionDTO actionDTO) {
        if (actionDTO.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (actionDTO.getResourceId() == null) {
            return ResponseEntity.badRequest().build();
        }
        if (actionDTO.getActionType() == null || actionDTO.getActionType().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        ResourceAction action = modelMapper.map(actionDTO, ResourceAction.class);
        String normalizedActionType = action.getActionType().trim().toUpperCase(Locale.ROOT);
        action.setActionType(normalizedActionType);

        if (action.getResourceTitle() == null || action.getResourceTitle().isEmpty()) {
            resourceRepository.findById(action.getResourceId())
                    .ifPresent(r -> action.setResourceTitle(r.getTitle()));
        }

        Optional<com.example.education_library.model.Resource> resourceOpt = resourceRepository.findById(action.getResourceId());
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.status(404).build();
        }

        ResourceAction savedAction;
        if ("BOOKMARK".equals(normalizedActionType)) {
            savedAction = actionRepository.findByUserIdAndResourceIdAndActionType(
                    action.getUserId(), action.getResourceId(), normalizedActionType)
                    .orElseGet(() -> actionRepository.save(action));
        } else {
            savedAction = actionRepository.save(action);
        }

        if ("DOWNLOAD".equals(normalizedActionType)) {
            com.example.education_library.model.Resource resource = resourceOpt.get();
            resource.setDownloads((resource.getDownloads() == null ? 0 : resource.getDownloads()) + 1);
            resourceRepository.save(resource);
            userRepository.findById(savedAction.getUserId()).ifPresent(user -> {
                emailService.sendDownloadNotification(user.getEmail(), savedAction.getResourceTitle());
            });
        } else if ("SAVE".equals(normalizedActionType) || "BOOKMARK".equals(normalizedActionType)) {
            userRepository.findById(savedAction.getUserId()).ifPresent(user -> {
                emailService.sendSaveNotification(user.getEmail(), savedAction.getResourceTitle());
            });
        }

        return ResponseEntity.ok(modelMapper.map(savedAction, ResourceActionDTO.class));
    }

    @DeleteMapping("/action/{userId}/{resourceId}/{actionType}")
    @Operation(summary = "Remove a resource action (Un-bookmark)", operationId = "removeResourceAction")
    public ResponseEntity<Void> removeAction(@PathVariable Long userId, @PathVariable Long resourceId, @PathVariable String actionType) {
        actionRepository.findByUserIdAndResourceIdAndActionType(userId, resourceId, actionType)
                .ifPresent(actionRepository::delete);
        return ResponseEntity.ok().build();
    }

    @Hidden
    @GetMapping("/files/**")
    @Operation(summary = "Serve resource file (PDF, Notes, etc.) including subdirectories", operationId = "serveResourceFile")
    public ResponseEntity<Resource> serveFile(
            jakarta.servlet.http.HttpServletRequest request,
            @RequestParam(defaultValue = "false") boolean download,
            @RequestParam(required = false) String name
    ) {
        try {
            // Extract the full path after /api/resources/files/
            String path = (String) request.getAttribute(org.springframework.web.servlet.HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String filename = path.substring(path.indexOf("/files/") + 7);
            
            String cleanPath = filename.startsWith("/") ? filename.substring(1) : filename;
            Resource resource = fileStorageService.loadAsResource(cleanPath);
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            Path resolvedPath = fileStorageService.resolve(cleanPath);
            String contentType = Files.probeContentType(resolvedPath);
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            String dispositionName = buildDownloadFilename(name, resource.getFilename());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, (download ? "attachment" : "inline") + "; filename=\"" + dispositionName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String buildDownloadFilename(String requestedName, String actualFileName) {
        String safeRequestedName = requestedName == null ? "" : requestedName.trim().replaceAll("[\\\\/:*?\"<>|]+", "_");
        if (safeRequestedName.isBlank()) {
            return actualFileName;
        }

        int extensionIndex = actualFileName == null ? -1 : actualFileName.lastIndexOf('.');
        String extension = extensionIndex >= 0 ? actualFileName.substring(extensionIndex) : "";
        return UriUtils.decode(safeRequestedName, StandardCharsets.UTF_8) + extension;
    }
}
