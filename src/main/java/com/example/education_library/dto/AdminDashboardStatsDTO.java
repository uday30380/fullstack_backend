package com.example.education_library.dto;

public class AdminDashboardStatsDTO {
    private long totalResources;
    private long totalUsers;
    private long totalDownloads;
    private long totalFeedback;
    private long pendingFacultyCount;

    public AdminDashboardStatsDTO() {
    }

    public AdminDashboardStatsDTO(long totalResources, long totalUsers, long totalDownloads, long totalFeedback, long pendingFacultyCount) {
        this.totalResources = totalResources;
        this.totalUsers = totalUsers;
        this.totalDownloads = totalDownloads;
        this.totalFeedback = totalFeedback;
        this.pendingFacultyCount = pendingFacultyCount;
    }

    public long getTotalResources() {
        return totalResources;
    }

    public void setTotalResources(long totalResources) {
        this.totalResources = totalResources;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalDownloads() {
        return totalDownloads;
    }

    public void setTotalDownloads(long totalDownloads) {
        this.totalDownloads = totalDownloads;
    }

    public long getTotalFeedback() {
        return totalFeedback;
    }

    public void setTotalFeedback(long totalFeedback) {
        this.totalFeedback = totalFeedback;
    }

    public long getPendingFacultyCount() {
        return pendingFacultyCount;
    }

    public void setPendingFacultyCount(long pendingFacultyCount) {
        this.pendingFacultyCount = pendingFacultyCount;
    }
}
