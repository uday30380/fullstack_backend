package com.example.education_library.repository;

import com.example.education_library.model.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    @Query("SELECT r FROM Resource r WHERE r.status = 'Active' OR r.status IS NULL")
    List<Resource> findActiveResources();
    
    @Query("""
            SELECT r FROM Resource r
            WHERE (r.status = 'Active' OR r.status IS NULL)
              AND (
                LOWER(COALESCE(r.facultyPin, '')) = LOWER(:facultyPin)
                OR COALESCE(r.isGlobal, false) = true
                OR LOWER(COALESCE(r.facultyPin, '')) = 'admin'
                OR LOWER(COALESCE(r.uploader, '')) = 'admin'
              )
            """)
    List<Resource> findActiveResourcesByFacultyPin(@Param("facultyPin") String facultyPin);

    @Query("""
            SELECT r FROM Resource r
            WHERE r.isFeatured = true
              AND (r.status = 'Active' OR r.status IS NULL)
              AND (
                LOWER(COALESCE(r.facultyPin, '')) = LOWER(:facultyPin)
                OR COALESCE(r.isGlobal, false) = true
                OR LOWER(COALESCE(r.facultyPin, '')) = 'admin'
                OR LOWER(COALESCE(r.uploader, '')) = 'admin'
              )
            ORDER BY r.featuredOrder ASC
            """)
    List<Resource> findFeaturedByFacultyPin(@Param("facultyPin") String facultyPin);

    @Query("""
            SELECT r FROM Resource r
            WHERE (r.status = 'Active' OR r.status IS NULL)
              AND (
                COALESCE(r.isGlobal, false) = true
                OR LOWER(COALESCE(r.facultyPin, '')) = 'admin'
                OR LOWER(COALESCE(r.uploader, '')) = 'admin'
              )
            """)
    List<Resource> findOfficialAdminResources();

    @Query("""
            SELECT r FROM Resource r
            WHERE r.isFeatured = true
              AND (r.status = 'Active' OR r.status IS NULL)
              AND (
                COALESCE(r.isGlobal, false) = true
                OR LOWER(COALESCE(r.facultyPin, '')) = 'admin'
                OR LOWER(COALESCE(r.uploader, '')) = 'admin'
              )
            ORDER BY r.featuredOrder ASC
            """)
    List<Resource> findFeaturedOfficialAdminResources();

    List<Resource> findByStatus(String status);
    List<Resource> findTop5ByOrderByUploadDateDesc();
    
    List<Resource> findTop3ByIsFeaturedTrueOrderByFeaturedOrderAsc();
}
