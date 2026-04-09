package com.example.education_library.repository;

import com.example.education_library.model.ResourceAction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ResourceActionRepository extends JpaRepository<ResourceAction, Long> {
    List<ResourceAction> findByUserIdOrderByActionDateDesc(Long userId);
    List<ResourceAction> findByUserIdAndActionType(Long userId, String actionType);
    Optional<ResourceAction> findByUserIdAndResourceIdAndActionType(Long userId, Long resourceId, String actionType);
    long countByActionType(String actionType);
    void deleteByResourceId(Long resourceId);
}
