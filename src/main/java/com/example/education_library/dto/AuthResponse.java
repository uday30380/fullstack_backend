package com.example.education_library.dto;

public class AuthResponse {
    private String token;
    private String message;
    private Long id;
    private String email;
    private String name;
    private String role;
    private String dept;
    private String status;
    private String facultyPin;
    private String joinedPin;

    public AuthResponse() {
    }

    public AuthResponse(String message, String token) {
        this.message = message;
        this.token = token;
    }

    public AuthResponse(String token, String message, Long id, String email, String name, String role, String dept, String status, String facultyPin, String joinedPin) {
        this.token = token;
        this.message = message;
        this.id = id;
        this.email = email;
        this.name = name;
        this.role = role;
        this.dept = dept;
        this.status = status;
        this.facultyPin = facultyPin;
        this.joinedPin = joinedPin;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
