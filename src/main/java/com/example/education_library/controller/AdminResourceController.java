package com.example.education_library.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.education_library.dto.ResourceDTO;
import com.example.education_library.model.Resource;
import com.example.education_library.repository.FeedbackRepository;
import com.example.education_library.repository.ResourceActionRepository;
import com.example.education_library.repository.ResourceRepository;
import com.example.education_library.service.FileStorageService;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/admin/resources")
@Tag(name = "Admin Resource Management", description = "Privileged operations for academic asset management")
public class AdminResourceController {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private ResourceActionRepository actionRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get all academic resources in the global ledger", operationId = "adminGetAllResources")
    public List<ResourceDTO> getAll() {
        return resourceRepository.findAll().stream()
                .map(r -> modelMapper.map(r, ResourceDTO.class))
                .collect(Collectors.toList());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ingest a new academic asset into the repository", operationId = "adminCreateResource")
    public ResponseEntity<ResourceDTO> create(
            @RequestParam("title") String title,
            @RequestParam("subject") String subject,
            @RequestParam("department") String department,
            @RequestParam("type") String type,
            @RequestParam("description") String description,
            @RequestParam(value = "youtubeUrl", required = false) String youtubeUrl,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "uploader", required = false, defaultValue = "Admin") String uploader,
            @RequestParam(value = "facultyPin", required = false) String facultyPin,
            @RequestParam(value = "isGlobal", required = false, defaultValue = "true") Boolean isGlobal
    ) {
        try {
            Resource resource = new Resource();
            resource.setTitle(title);
            resource.setSubject(subject);
            resource.setDepartment(department);
            resource.setType(type);
            resource.setDescription(description);
            resource.setYoutubeUrl(youtubeUrl);
            resource.setUploader(uploader != null ? uploader : "Admin");
            resource.setFacultyPin(facultyPin);
            resource.setIsGlobal(isGlobal);
            resource.setStatus("Active");
            resource.setUploadDate(LocalDateTime.now());

            if (thumbnail != null && !thumbnail.isEmpty()) {
                resource.setThumbnailPath(fileStorageService.storeFile(thumbnail));
            }

            if (file != null && !file.isEmpty()) {
                resource.setResourcePath(fileStorageService.storeFile(file));
                resource.setSize((file.getSize() / 1024) + " KB");
            }

            Resource saved = resourceRepository.save(resource);
            return ResponseEntity.status(HttpStatus.CREATED).body(modelMapper.map(saved, ResourceDTO.class));
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Resource Ingestion failed -> " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Modify existing academic asset metadata or content (File Upgrade)", operationId = "adminUpdateResourceMultipart")
    public ResponseEntity<ResourceDTO> update(
            @PathVariable Long id,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "youtubeUrl", required = false) String youtubeUrl,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "uploader", required = false) String uploader,
            @RequestParam(value = "facultyPin", required = false) String facultyPin,
            @RequestParam(value = "isGlobal", required = false) Boolean isGlobal
    ) {
        java.util.Optional<Resource> resourceOpt = resourceRepository.findById(id);
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        Resource resource = resourceOpt.get();
        try {
            if (title != null) resource.setTitle(title);
            if (subject != null) resource.setSubject(subject);
            if (department != null) resource.setDepartment(department);
            if (type != null) resource.setType(type);
            if (description != null) resource.setDescription(description);
            if (youtubeUrl != null) resource.setYoutubeUrl(youtubeUrl);
            if (uploader != null) resource.setUploader(uploader);
            if (facultyPin != null) resource.setFacultyPin(facultyPin);
            if (isGlobal != null) resource.setIsGlobal(isGlobal);

            if (thumbnail != null && !thumbnail.isEmpty()) {
                if (resource.getThumbnailPath() != null) {
                    fileStorageService.deleteFile(resource.getThumbnailPath());
                }
                resource.setThumbnailPath(fileStorageService.storeFile(thumbnail));
            }

            if (file != null && !file.isEmpty()) {
                if (resource.getResourcePath() != null) {
                    fileStorageService.deleteFile(resource.getResourcePath());
                }
                resource.setResourcePath(fileStorageService.storeFile(file));
                resource.setSize((file.getSize() / 1024) + " KB");
            }

            Resource updated = resourceRepository.save(resource);
            return ResponseEntity.ok(modelMapper.map(updated, ResourceDTO.class));
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR: Resource Modification failed -> " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify existing academic asset metadata without file uploads (JSON Mode)", operationId = "adminUpdateResourceJson")
    public ResponseEntity<ResourceDTO> updateJson(@PathVariable Long id, @RequestBody ResourceDTO jsonUpdate) {
        return updateMetadataInternal(id, jsonUpdate);
    }

    @PutMapping(value = "/{id}/metadata", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Modify existing academic asset metadata without file uploads (JSON Mode)", operationId = "adminUpdateResourceMetadata")
    public ResponseEntity<ResourceDTO> updateMetadata(@PathVariable Long id, @RequestBody ResourceDTO jsonUpdate) {
        return updateMetadataInternal(id, jsonUpdate);
    }

    private ResponseEntity<ResourceDTO> updateMetadataInternal(Long id, ResourceDTO jsonUpdate) {
        java.util.Optional<Resource> resourceOpt = resourceRepository.findById(id);
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        Resource resource = resourceOpt.get();
        try {
            if (jsonUpdate.getTitle() != null) resource.setTitle(jsonUpdate.getTitle());
            if (jsonUpdate.getSubject() != null) resource.setSubject(jsonUpdate.getSubject());
            if (jsonUpdate.getDepartment() != null) resource.setDepartment(jsonUpdate.getDepartment());
            if (jsonUpdate.getType() != null) resource.setType(jsonUpdate.getType());
            if (jsonUpdate.getDescription() != null) resource.setDescription(jsonUpdate.getDescription());
            if (jsonUpdate.getYoutubeUrl() != null) resource.setYoutubeUrl(jsonUpdate.getYoutubeUrl());
            if (jsonUpdate.getUploader() != null) resource.setUploader(jsonUpdate.getUploader());
            if (jsonUpdate.getFacultyPin() != null) resource.setFacultyPin(jsonUpdate.getFacultyPin());
            if (jsonUpdate.getIsGlobal() != null) resource.setIsGlobal(jsonUpdate.getIsGlobal());
            if (jsonUpdate.getStatus() != null) resource.setStatus(jsonUpdate.getStatus());

            Resource updated = resourceRepository.save(resource);
            return ResponseEntity.ok(modelMapper.map(updated, ResourceDTO.class));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @DeleteMapping("/{id}")
    @Transactional
    @Operation(summary = "Permanently expunge an academic asset and its scholarly history", operationId = "adminDeleteResource")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        java.util.Optional<Resource> resourceOpt = resourceRepository.findById(id);
        if (resourceOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        Resource resource = resourceOpt.get();
        try {
            actionRepository.deleteByResourceId(id);
            feedbackRepository.deleteByResourceId(id);
            
            if (resource.getThumbnailPath() != null) {
                fileStorageService.deleteFile(resource.getThumbnailPath());
            }
            if (resource.getResourcePath() != null) {
                fileStorageService.deleteFile(resource.getResourcePath());
            }
            
            resourceRepository.delete(resource);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}/feature")
    @Operation(summary = "Manage featured status and display orientation of an asset", operationId = "adminSetFeatured")
    public ResponseEntity<ResourceDTO> setFeatured(
            @PathVariable Long id,
            @RequestParam("isFeatured") Boolean isFeatured,
            @RequestParam(value = "order", defaultValue = "0") Integer order
    ) {
        try {
            return resourceRepository.findById(id).map(resource -> {
                int featureOrder = order != null ? order : 0;
                if (Boolean.TRUE.equals(isFeatured) && featureOrder > 0) {
                    List<Resource> others = resourceRepository.findTop3ByIsFeaturedTrueOrderByFeaturedOrderAsc();
                    for (Resource other : others) {
                        if (other.getFeaturedOrder() != null && other.getFeaturedOrder().equals(featureOrder) && !other.getId().equals(id)) {
                            other.setIsFeatured(false);
                            other.setFeaturedOrder(0);
                            resourceRepository.save(other);
                        }
                    }
                }
                
                resource.setIsFeatured(Boolean.TRUE.equals(isFeatured));
                resource.setFeaturedOrder(Boolean.TRUE.equals(isFeatured) ? featureOrder : 0);
                Resource updated = resourceRepository.save(resource);
                return ResponseEntity.ok(modelMapper.map(updated, ResourceDTO.class));
            }).orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Hidden
    @GetMapping("/files/{filename}")
    @Operation(summary = "Direct link delivery for administrative file verification", operationId = "adminServeFile")
    public ResponseEntity<org.springframework.core.io.Resource> serveFile(@PathVariable String filename) {
        try {
            org.springframework.core.io.Resource resource = fileStorageService.loadAsResource(filename);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
