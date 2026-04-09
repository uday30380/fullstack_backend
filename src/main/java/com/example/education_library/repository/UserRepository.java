package com.example.education_library.repository;

import com.example.education_library.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByName(String name);
    List<User> findTop5ByOrderByIdDesc();
    Optional<User> findByFacultyPin(String pin);
    long countByRoleAndStatus(String role, String status);
}
