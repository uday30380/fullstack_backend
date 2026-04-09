package com.example.education_library.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subject;
    private String department;
    private String type; // Textbook, Notes, Video
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    private Double rating = 5.0;
    private String uploader;
    private LocalDateTime uploadDate;
    
    private Integer downloads = 0;
    private String size;
    private Integer pages = 0;
    private String status = "Active";

    private String youtubeUrl;
    private String thumbnailPath;
    private String resourcePath; // Path to PDF/Notes on disk

    @Column(name = "is_featured")
    private Boolean isFeatured = false;

    @Column(name = "featured_order")
    private Integer featuredOrder = 0;

    @Column(name = "faculty_pin")
    private String facultyPin; // For faculty/student node synchronization

    @Column(name = "is_global")
    private Boolean isGlobal = true; // For Admin-uploaded universal assets

    @PrePersist
    protected void onCreate() {
        uploadDate = LocalDateTime.now();
    }

    public Resource() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getUploader() {
        return uploader;
    }

    public void setUploader(String uploader) {
        this.uploader = uploader;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Integer getDownloads() {
        return downloads;
    }

    public void setDownloads(Integer downloads) {
        this.downloads = downloads;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public void setYoutubeUrl(String youtubeUrl) {
        this.youtubeUrl = youtubeUrl;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public Boolean getIsFeatured() {
        return isFeatured;
    }

    public void setIsFeatured(Boolean featured) {
        isFeatured = featured;
    }

    public Integer getFeaturedOrder() {
        return featuredOrder;
    }

    public void setFeaturedOrder(Integer featuredOrder) {
        this.featuredOrder = featuredOrder;
    }

    public String getFacultyPin() {
        return facultyPin;
    }

    public void setFacultyPin(String facultyPin) {
        this.facultyPin = facultyPin;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(Boolean global) {
        isGlobal = global;
    }
}
