package com.example.education_library.service;

import com.example.education_library.model.Resource;
import com.example.education_library.model.User;
import com.example.education_library.repository.ResourceRepository;
import com.example.education_library.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        logger.info("DataInitializer: Initializing test data environment...");
        
        initializeUsers();
        initializeResources();
        repairMetadataOnlyResources();
        
        logger.info("DataInitializer: Scholastic data ledger synchronization complete.");
    }

    private void initializeUsers() {
        if (userRepository.count() > 0) {
            logger.info("DataInitializer: User registry already contains data. Skipping default user creation.");
            return;
        }

        // 1. Admin Account
        createUser("Admin User", "admin@edulib.com", "Test@123456", "Admin", null, "ADMIN-001");

        // 2. Faculty Account
        createUser("Dr. Sarah Wilson", "faculty@edulib.com", "Test@123456", "Faculty", "Computer Science", "FAC-001");

        // 3. Student Accounts
        createUser("John Doe", "student1@edulib.com", "Test@123456", "Student", "Computer Science", "STU-001");
        createUser("Jane Smith", "student2@edulib.com", "Test@123456", "Student", "Electronics", "STU-002");
        createUser("Mike Johnson", "student3@edulib.com", "Test@123456", "Student", "Mechanical", "STU-003");

        logger.info("DataInitializer: Successfully initialized 5 test users.");
    }

    private void createUser(String name, String email, String password, String role, String dept, String idNumber) {
        User user = new User();
        user.setName(name);
        user.setEmail(email.toLowerCase());
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setDept(dept);
        user.setIdNumber(idNumber);
        user.setStatus("Active");
        
        if ("Faculty".equalsIgnoreCase(role)) {
            user.setFacultyPin("123456"); // Fixed PIN for testing
        }
        
        userRepository.save(user);
    }

    private void initializeResources() {
        if (resourceRepository.count() > 0) {
            logger.info("DataInitializer: Resource repository already contains data. Skipping sample resource creation.");
            return;
        }

        createResource("Introduction to Data Structures", "Data Structures", "Computer Science", "PDF", 
                "Comprehensive guide to fundamental data structures.", "Admin", true);
        
        createResource("Object Oriented Programming", "Programming", "Computer Science", "PDF", 
                "Advanced concepts in OOP using Java and C++.", "Admin", true);
        
        createResource("Database Design Fundamentals", "Database", "Computer Science", "PDF", 
                "Learn how to design efficient relational databases.", "Admin", true);
        
        createResource("Web Development with React", "Web Technology", "Computer Science", "PDF", 
                "Build modern, responsive UIs with React and Vite.", "Admin", true);

        logger.info("DataInitializer: Successfully initialized 4 sample resources.");
    }

    private void createResource(String title, String subject, String dept, String type, String desc, String uploader, boolean isFeatured) {
        Resource resource = new Resource();
        resource.setTitle(title);
        resource.setSubject(subject);
        resource.setDepartment(dept);
        resource.setType(type);
        resource.setDescription(desc);
        resource.setUploader(uploader);
        resource.setStatus("Active");
        resource.setIsGlobal(true);
        resource.setIsFeatured(isFeatured);
        resource.setFeaturedOrder(isFeatured ? 1 : 0);
        resource.setUploadDate(LocalDateTime.now());
        attachGeneratedResourceFile(resource);
        
        resourceRepository.save(resource);
    }

    private void repairMetadataOnlyResources() {
        resourceRepository.findAll().stream()
                .filter(resource -> resource.getResourcePath() == null || resource.getResourcePath().isBlank())
                .filter(resource -> resource.getYoutubeUrl() == null || resource.getYoutubeUrl().isBlank())
                .forEach(resource -> {
                    attachGeneratedResourceFile(resource);
                    resourceRepository.save(resource);
                    logger.info("DataInitializer: Attached generated download file for resource '{}'.", resource.getTitle());
                });
    }

    private void attachGeneratedResourceFile(Resource resource) {
        String slug = slugify(resource.getTitle());
        String relativePath = "generated/" + slug + ".txt";
        String content = """
                %s

                Subject: %s
                Department: %s
                Type: %s

                %s

                This generated file keeps seeded and metadata-only resources downloadable.
                Replace it by uploading the real PDF, notes, or video from the admin resource panel.
                """.formatted(
                nullToText(resource.getTitle(), "Education Library Resource"),
                nullToText(resource.getSubject(), "General"),
                nullToText(resource.getDepartment(), "General"),
                nullToText(resource.getType(), "Notes"),
                nullToText(resource.getDescription(), "No description available."));

        String storedPath = fileStorageService.storeTextFile(relativePath, content);
        resource.setResourcePath(storedPath);
        resource.setSize((fileStorageService.fileSize(storedPath) / 1024 + 1) + " KB");
        if (resource.getType() == null || resource.getType().isBlank() || "PDF".equalsIgnoreCase(resource.getType())) {
            resource.setType("Notes");
        }
    }

    private String slugify(String value) {
        String slug = value == null ? "resource" : value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "resource" : slug;
    }

    private String nullToText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
