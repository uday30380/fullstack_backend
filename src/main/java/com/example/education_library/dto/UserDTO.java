package com.example.education_library.dto;

public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String role;
    private String dept;
    private String idNumber;
    private String status;
    private String avatarPath;
    private String facultyPin;
    private String joinedPin;

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
