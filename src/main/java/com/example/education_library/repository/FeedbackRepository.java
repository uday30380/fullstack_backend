package com.example.education_library.repository;

import com.example.education_library.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByResourceIdOrderByDateDesc(Long resourceId);
    void deleteByResourceId(Long resourceId);
    List<Feedback> findTop5ByOrderByDateDesc();
    List<Feedback> findByUserIdOrderByDateDesc(Long userId);
    @Query("select avg(f.rating) from Feedback f where f.resourceId = :resourceId")
    Double findAverageRatingByResourceId(@Param("resourceId") Long resourceId);
}
