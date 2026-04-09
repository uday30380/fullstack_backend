package com.example.education_library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    private String role; // Student, Faculty, Admin
    private String dept;
    private String idNumber;
    private String status = "Pending"; // Pending, Active, Suspended
    private String avatarPath; // Institutional Profile Picture Reference
    private String resetToken; // OTP/Token for password recovery
    private LocalDateTime resetTokenExpiry;
    
    private String emailVerificationCode;
    private LocalDateTime emailVerificationCodeExpiry;
    
    @Column(name = "faculty_pin")
    private String facultyPin; // Access PIN for this faculty (only if role is Faculty)
    private String joinedPin; // PIN of the faculty this student has joined

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public LocalDateTime getResetTokenExpiry() {
        return resetTokenExpiry;
    }

    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) {
        this.resetTokenExpiry = resetTokenExpiry;
    }

    public String getFacultyPin() {
        return facultyPin;
    }

    public void setFacultyPin(String facultyPin) {
        this.facultyPin = facultyPin;
    }

    public String getJoinedPin() {
        return joinedPin;
    }

    public void setJoinedPin(String joinedPin) {
        this.joinedPin = joinedPin;
    }

    public String getEmailVerificationCode() {
        return emailVerificationCode;
    }

    public void setEmailVerificationCode(String emailVerificationCode) {
        this.emailVerificationCode = emailVerificationCode;
    }

    public LocalDateTime getEmailVerificationCodeExpiry() {
        return emailVerificationCodeExpiry;
    }

    public void setEmailVerificationCodeExpiry(LocalDateTime emailVerificationCodeExpiry) {
        this.emailVerificationCodeExpiry = emailVerificationCodeExpiry;
    }
}
