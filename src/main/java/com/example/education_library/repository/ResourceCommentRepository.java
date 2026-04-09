package com.example.education_library.repository;

import com.example.education_library.model.ResourceComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ResourceCommentRepository extends JpaRepository<ResourceComment, Long> {
    List<ResourceComment> findByResourceIdOrderByDateDesc(Long resourceId);
}
