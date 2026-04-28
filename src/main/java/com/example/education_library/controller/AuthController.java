package com.example.education_library.controller;

import com.example.education_library.config.AppProperties;
import com.example.education_library.dto.AuthRequest;
import com.example.education_library.dto.AuthResponse;
import com.example.education_library.dto.RegistrationRequest;
import com.example.education_library.dto.UserDTO;
import com.example.education_library.dto.ForgotPasswordRequest;
import com.example.education_library.dto.ResetPasswordRequest;
import com.example.education_library.dto.VerifyEmailRequest;
import com.example.education_library.dto.JoinFacultyRequest;

import com.example.education_library.model.User;
import com.example.education_library.repository.UserRepository;
import com.example.education_library.security.JwtUtil;
import com.example.education_library.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication Management", description = "Operations related to user onboarding, session validation, and profile management")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping("/register")
    @Operation(summary = "Establish a new scholastic identity node (Registration)", operationId = "authRegister")
    public ResponseEntity<AuthResponse> register(@RequestBody RegistrationRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        System.out.println("Scholastic Identity Node Initialized for: " + normalizedEmail);
        
        try {
            if (normalizedEmail == null || normalizedEmail.isBlank()) {
                return ResponseEntity.badRequest().body(new AuthResponse("Institutional email is required.", null));
            }

            java.util.Optional<User> existingOpt = userRepository.findByEmailIgnoreCase(normalizedEmail);
            if (existingOpt.isPresent()) {
                User existing = existingOpt.get();
                if ("Active".equalsIgnoreCase(existing.getStatus())) {
                    System.out.println("ALERT: Duplicate Identity detected for " + normalizedEmail);
                    return ResponseEntity.status(409).body(new AuthResponse("A scholar with this email already exists in the central repository.", null));
                }
                return ResponseEntity.status(403)
                        .body(new AuthResponse("Your account exists but is pending administrative approval.", null));
            }

            if ("Admin".equalsIgnoreCase(request.getRole())
                    && !appProperties.getSecurity().getAdminSecret().equals(request.getSecretCode())) {
                return ResponseEntity.status(401).body(new AuthResponse("Invalid administrator secret code.", null));
            }

            if ("Faculty".equalsIgnoreCase(request.getRole())) {
                if (request.getFacultyPin() == null || request.getFacultyPin().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(new AuthResponse("Institutional Faculty PIN is required for this node role.", null));
                }
                if (userRepository.findByFacultyPin(request.getFacultyPin()).isPresent()) {
                    return ResponseEntity.status(409).body(new AuthResponse("Conflict: This Faculty PIN is already allocated to another node.", null));
                }
            }

            User user = modelMapper.map(request, User.class);
            user.setEmail(normalizedEmail);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmailVerificationCode(null);
            user.setEmailVerificationCodeExpiry(null);
            if ("Faculty".equalsIgnoreCase(request.getRole()) && request.getFacultyPin() != null) {
                user.setFacultyPin(request.getFacultyPin().trim());
                user.setStatus("Pending");
            } else {
                user.setStatus("Active");
            }
            User savedUser = userRepository.save(user);

            if ("Faculty".equalsIgnoreCase(savedUser.getRole())) {
                emailService.sendFacultyApplicationEmail(savedUser.getEmail(), savedUser.getName());
                emailService.notifyAdminOfNewFaculty(savedUser.getName(), savedUser.getEmail());
                AuthResponse pendingResponse = modelMapper.map(savedUser, AuthResponse.class);
                pendingResponse.setMessage("Faculty account created and is pending administrative approval.");
                return ResponseEntity.ok(pendingResponse);
            }

            emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());
            AuthResponse response = modelMapper.map(savedUser, AuthResponse.class);
            response.setToken(jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole()));
            response.setMessage("Registration complete. You are signed in.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("Registration Protocol failure: " + e.getMessage());
            return ResponseEntity.status(500).body(new AuthResponse("Critical Registry Error: Identity could not be finalized.", null));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Synchronize existing identity credentials for access protocol (Login)", operationId = "authLogin")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest loginRequest) {
        String normalizedEmail = normalizeEmail(loginRequest.getEmail());
        Optional<User> userOpt = findUserByEmail(normalizedEmail);

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, loginRequest.getPassword())
            );
        } catch (AuthenticationException e) {
            if (userOpt.isPresent() && isLegacyPasswordMatch(userOpt.get(), loginRequest.getPassword())) {
                User legacyUser = userOpt.get();
                legacyUser.setPassword(passwordEncoder.encode(loginRequest.getPassword()));
                userRepository.save(legacyUser);
            } else {
                return ResponseEntity.status(401).body(new AuthResponse("Invalid email or password.", null));
            }
        }

        User user = findUserByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Unexpected data failure during sync."));
        
        if ("Admin".equalsIgnoreCase(user.getRole())) {
            if (!appProperties.getSecurity().getAdminSecret().equals(loginRequest.getSecretCode())) {
                 return ResponseEntity.status(401).body(new AuthResponse("Invalid administrator secret code.", null));
            }
        }
        
        if ("Faculty".equalsIgnoreCase(user.getRole()) && (user.getFacultyPin() == null || user.getFacultyPin().isEmpty())) {
            user.setFacultyPin(String.format("%06d", new Random().nextInt(1000000)));
            userRepository.save(user);
        }

        if (!"Active".equalsIgnoreCase(user.getStatus())) {
            return ResponseEntity.status(403).body(new AuthResponse("Your account is pending approval. Please contact the administrator.", null));
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        emailService.sendLoginNotification(user.getEmail(), user.getName());
        AuthResponse response = modelMapper.map(user, AuthResponse.class);
        response.setToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/join-faculty")
    @Operation(summary = "Affiliate student identity with an academic faculty node", operationId = "authJoinFaculty")
    public ResponseEntity<AuthResponse> joinFaculty(@RequestBody JoinFacultyRequest request) {
        String studentEmail = normalizeEmail(request.getEmail());
        String pin = request.getPin();

        Optional<User> studentOpt = findUserByEmail(studentEmail);
        if (!studentOpt.isPresent()) return ResponseEntity.badRequest().build();

        User student = studentOpt.get();
        if (pin == null || pin.trim().isEmpty()) {
            student.setJoinedPin(null);
            userRepository.save(student);
            return ResponseEntity.ok(modelMapper.map(student, AuthResponse.class));
        }

        Optional<User> facultyOpt = userRepository.findByFacultyPin(pin);
        if (!facultyOpt.isPresent()) {
            return ResponseEntity.status(404).body(new AuthResponse("Faculty Node Not Found: The provided PIN does not correspond to any registered institutional entity.", null));
        }

        student.setJoinedPin(pin);
        userRepository.save(student);

        return ResponseEntity.ok(modelMapper.map(student, AuthResponse.class));
    }

    @GetMapping("/users")
    @Operation(summary = "Retrieve every registered identity for administrative oversight", operationId = "authGetAllUsers")
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .collect(Collectors.toList());
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Update a registered identity's public metadata or status", operationId = "authUpdateUser")
    public ResponseEntity<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userUpdate) {
        return userRepository.findById(id).map(user -> {
            if (userUpdate.getName() != null && !userUpdate.getName().isBlank()) {
                user.setName(userUpdate.getName().trim());
            }
            if (userUpdate.getDept() != null) {
                user.setDept(userUpdate.getDept().trim());
            }
            if (userUpdate.getIdNumber() != null) {
                user.setIdNumber(userUpdate.getIdNumber().trim());
            }
            if (userUpdate.getStatus() != null && !userUpdate.getStatus().isBlank()) {
                user.setStatus(userUpdate.getStatus().trim());
            }
            if (userUpdate.getJoinedPin() != null) {
                user.setJoinedPin(userUpdate.getJoinedPin().isBlank() ? null : userUpdate.getJoinedPin().trim());
            }
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

            User savedUser = userRepository.save(user);
            return ResponseEntity.ok(modelMapper.map(savedUser, UserDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/profile/{email:.+}")
    @Operation(summary = "Retrieve an identity profile by institutional email", operationId = "authGetProfile")
    public ResponseEntity<UserDTO> getProfile(@PathVariable String email) {
        return findUserByEmail(email)
                .map(user -> ResponseEntity.ok(modelMapper.map(user, UserDTO.class)))
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Terminate an identity node from the registry", operationId = "authDeleteUser")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate identity recovery protocol for lost credentials", operationId = "authForgotPassword")
    public ResponseEntity<String> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Institutional email is required.");
        }
        Optional<User> userOpt = findUserByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String otp = String.format("%06d", new Random().nextInt(1000000));
            user.setResetToken(otp);
            user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(email, otp);
        }
        
        return ResponseEntity.ok("Recovery protocol initialized if account exists.");
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify scholastic identity via email-dispatched code", operationId = "authVerifyEmail")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestBody VerifyEmailRequest request) {
        String email = normalizeEmail(request.getEmail());
        String code = request.getCode();

        if (email == null || code == null) {
            return ResponseEntity.badRequest().build();
        }

        // Look up user from DB (OTP is now stored in DB, not memory)
        java.util.Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (code.equals(user.getEmailVerificationCode())
                    && user.getEmailVerificationCodeExpiry() != null
                    && user.getEmailVerificationCodeExpiry().isAfter(LocalDateTime.now())) {

                // Clear OTP
                user.setEmailVerificationCode(null);
                user.setEmailVerificationCodeExpiry(null);

                if ("Admin".equalsIgnoreCase(user.getRole())) {
                    // For admin registered via UI, secretCode was validated at registration time
                    user.setStatus("Active");
                } else if ("Faculty".equalsIgnoreCase(user.getRole())) {
                    user.setStatus("Pending");
                    User savedUser = userRepository.save(user);
                    emailService.sendFacultyApplicationEmail(user.getEmail(), user.getName());
                    emailService.notifyAdminOfNewFaculty(user.getName(), user.getEmail());
                    AuthResponse pendingResponse = modelMapper.map(savedUser, AuthResponse.class);
                    return ResponseEntity.ok(pendingResponse);
                } else {
                    user.setStatus("Active");
                    emailService.sendWelcomeEmail(user.getEmail(), user.getName());
                }

                User savedUser = userRepository.save(user);
                AuthResponse response = modelMapper.map(savedUser, AuthResponse.class);
                if ("Active".equalsIgnoreCase(savedUser.getStatus())) {
                    String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getRole());
                    response.setToken(token);
                }
                return ResponseEntity.ok(response);
            }
        }
        
        return ResponseEntity.status(400).body(new AuthResponse("Invalid or expired scholastic verification code. Please request a new one.", null));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Re-dispatch scholastic identity verification code", operationId = "authResendVerification")
    public ResponseEntity<AuthResponse> resendVerification(@RequestBody ForgotPasswordRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (email == null) return ResponseEntity.badRequest().build();

        // Look up pending user from DB
        java.util.Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isPresent() && !"Active".equalsIgnoreCase(userOpt.get().getStatus())) {
            User user = userOpt.get();
            String otp = String.format("%06d", new Random().nextInt(1000000));
            user.setEmailVerificationCode(otp);
            user.setEmailVerificationCodeExpiry(LocalDateTime.now().plusMinutes(30));
            userRepository.save(user);
            emailService.sendEmailVerificationCode(email, otp);
            return ResponseEntity.ok(new AuthResponse("Verification code re-dispatched to " + email, null));
        }
        return ResponseEntity.status(400).body(new AuthResponse("Resend rejected. No pending identity synchronization found for this node.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Complete identity recovery by establishing new credentials", operationId = "authResetPassword")
    public ResponseEntity<String> resetPassword(@RequestBody ResetPasswordRequest request) {
        String otp = request.getToken();
        String email = normalizeEmail(request.getEmail());
        String newPassword = request.getNewPassword();
        // oldPassword is captured for legacy protocol compatibility but reset is authorized via OTP
        String oldPassword = request.getOldPassword();

        if (otp == null || otp.isBlank() || newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().body("Recovery OTP and new password are required.");
        }

        // Locate identity node by OTP synchronization
        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> otp.equals(u.getResetToken()) &&
                            u.getResetTokenExpiry() != null &&
                            u.getResetTokenExpiry().isAfter(LocalDateTime.now()) &&
                            (email == null || email.isBlank() || email.equalsIgnoreCase(u.getEmail())))
                .findFirst();
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            // Log verification of identity transition
            System.out.println("Identity transition authorized for: " + user.getEmail());
            
            // Overwrite existing credentials with new protocol
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setResetToken(null);
            user.setResetTokenExpiry(null);
            userRepository.save(user);
            
            return ResponseEntity.ok("Identity restored. Existing credentials overwritten successfully.");
        }
        
        return ResponseEntity.status(400).body("Invalid or expired recovery OTP. Identity could not be verified.");
    }

    private Optional<User> findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private boolean isLegacyPasswordMatch(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        return storedPassword != null
                && rawPassword != null
                && !storedPassword.startsWith("$2")
                && storedPassword.equals(rawPassword);
    }
}
