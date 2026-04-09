package com.example.education_library.controller;

import com.example.education_library.dto.UserDTO;
import com.example.education_library.model.User;
import com.example.education_library.repository.UserRepository;
import com.example.education_library.service.EmailService;
import com.example.education_library.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Profile Management", description = "Operations related to scholastic identity refinement and personal data")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ModelMapper modelMapper;

    @PutMapping("/{id}")
    @Operation(summary = "Modify scholastic identity metadata for a specific node", operationId = "updateUserProfile")
    public ResponseEntity<UserDTO> updateProfile(@PathVariable Long id, @RequestBody UserDTO userUpdate) {
        return userRepository.findById(id).map(user -> {
            if (userUpdate.getFacultyPin() != null) {
                String requestedPin = userUpdate.getFacultyPin().trim();
                if (!requestedPin.isEmpty()
                        && userRepository.findByFacultyPin(requestedPin)
                        .filter(existing -> !existing.getId().equals(id))
                        .isPresent()) {
                    return ResponseEntity.status(409).<UserDTO>build();
                }
                user.setFacultyPin(requestedPin.isEmpty() ? null : requestedPin);
            }
            user.setName(userUpdate.getName());
            user.setDept(userUpdate.getDept());
            user.setIdNumber(userUpdate.getIdNumber());
            User savedUser = userRepository.save(user);
            emailService.sendProfileUpdateNotification(savedUser.getEmail(), savedUser.getName());
            return ResponseEntity.ok(modelMapper.map(savedUser, UserDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/avatar")
    @Operation(summary = "Establish or refresh the institutional profile picture reference", operationId = "uploadUserAvatar")
    public ResponseEntity<UserDTO> uploadAvatar(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return userRepository.findById(id).map(user -> {
            try {
                if (user.getAvatarPath() != null && !user.getAvatarPath().isBlank()) {
                    fileStorageService.deleteFile(user.getAvatarPath());
                }

                user.setAvatarPath(fileStorageService.storeFile(file, "avatars"));
                User savedUser = userRepository.save(user);
                return ResponseEntity.ok(modelMapper.map(savedUser, UserDTO.class));
            } catch (RuntimeException e) {
                return ResponseEntity.internalServerError().<UserDTO>build();
            }
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve identity node details by institutional ID", operationId = "getUserNodeDetails")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(user -> ResponseEntity.ok(modelMapper.map(user, UserDTO.class)))
                .orElse(ResponseEntity.notFound().build());
    }
}
