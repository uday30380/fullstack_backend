package com.example.education_library.controller;

import com.example.education_library.config.AppProperties;
import com.example.education_library.dto.FeedbackDTO;
import com.example.education_library.model.Feedback;
import com.example.education_library.model.Resource;
import com.example.education_library.model.User;
import com.example.education_library.repository.FeedbackRepository;
import com.example.education_library.repository.ResourceRepository;
import com.example.education_library.repository.UserRepository;
import com.example.education_library.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feedback")
@Tag(name = "Feedback Management", description = "Operations related to scholarly feedback and asset ratings")
public class FeedbackController {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackController.class);

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private AppProperties appProperties;

    @Autowired
    private ModelMapper modelMapper;

    @PostMapping
    @Operation(summary = "Submit a new scholarly feedback entry", operationId = "addFeedback")
    public ResponseEntity<FeedbackDTO> addFeedback(@RequestBody FeedbackDTO feedbackDTO) {
        if (feedbackDTO.getRating() == null || feedbackDTO.getRating() < 1 || feedbackDTO.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }
        if (feedbackDTO.getContent() == null || feedbackDTO.getContent().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Resource> resourceOpt = Optional.empty();
        if (feedbackDTO.getResourceId() != null) {
            resourceOpt = resourceRepository.findById(feedbackDTO.getResourceId());
            if (resourceOpt.isEmpty()) {
                return ResponseEntity.status(404).build();
            }
        }

        Feedback feedback = modelMapper.map(feedbackDTO, Feedback.class);
        if (resourceOpt.isPresent()) {
            feedback.setResourceTitle(resourceOpt.get().getTitle());
        } else if (feedback.getResourceTitle() == null || feedback.getResourceTitle().trim().isEmpty()) {
            feedback.setResourceTitle("General Feedback");
        }
        Feedback saved = feedbackRepository.save(feedback);

        if (resourceOpt.isPresent()) {
            Double averageRating = feedbackRepository.findAverageRatingByResourceId(saved.getResourceId());
            Resource resource = resourceOpt.get();
            resource.setRating(averageRating != null ? Math.round(averageRating * 10.0) / 10.0 : resource.getRating());
            resourceRepository.save(resource);
        }
        
        try {
            String reviewerEmail = null;
            if (saved.getUserEmail() != null && saved.getUserEmail().contains("@")) {
                reviewerEmail = saved.getUserEmail();
            } else if (saved.getUserId() != null) {
                reviewerEmail = userRepository.findById(saved.getUserId())
                        .map(User::getEmail)
                        .orElse(null);
            }
            if (reviewerEmail != null) {
                emailService.sendReviewNotification(reviewerEmail, saved.getResourceTitle());
            }

            final String currentReviewerEmail = reviewerEmail;
            if (saved.getResourceId() != null) {
                resourceRepository.findById(saved.getResourceId()).ifPresent(savedResource -> {
                    String uploaderEmail = null;
                    String uploaderId = savedResource.getUploader();
                    
                    if (uploaderId != null && uploaderId.contains("@")) {
                        uploaderEmail = uploaderId;
                    } else if (uploaderId != null) {
                        uploaderEmail = userRepository.findByName(uploaderId)
                                .map(User::getEmail)
                                .orElse(null);
                    }
                    
                    if (uploaderEmail == null) uploaderEmail = appProperties.getMail().getAdminEmail();
                    
                    emailService.notifyUploaderOfReview(
                        uploaderEmail, 
                        savedResource.getTitle(), 
                        currentReviewerEmail != null ? currentReviewerEmail : "A Scholar", 
                        saved.getRating()
                    );
                });
            } else {
                emailService.notifyUploaderOfReview(
                    appProperties.getMail().getAdminEmail(),
                    saved.getResourceTitle(),
                    currentReviewerEmail != null ? currentReviewerEmail : "A Scholar",
                    saved.getRating()
                );
            }
        } catch (Exception e) {
            logger.warn("FEEDBACK NOTIFICATION ERROR: " + e.getMessage(), e);
        }
        
        return ResponseEntity.ok(modelMapper.map(saved, FeedbackDTO.class));
    }

    @GetMapping
    @Operation(summary = "Get all feedback entries", operationId = "getFeedbackAll")
    public List<FeedbackDTO> getFeedback() {
        return feedbackRepository.findAll().stream()
                .map(f -> modelMapper.map(f, FeedbackDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/resource/{resourceId}")
    @Operation(summary = "Get feedback entries for a specific asset", operationId = "getFeedbackByResource")
    public List<FeedbackDTO> getFeedbackByResource(@PathVariable Long resourceId) {
        return feedbackRepository.findByResourceIdOrderByDateDesc(resourceId).stream()
                .map(f -> modelMapper.map(f, FeedbackDTO.class))
                .collect(Collectors.toList());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get feedback entries submitted by a specific user", operationId = "getFeedbackByUser")
    public List<FeedbackDTO> getFeedbackByUser(@PathVariable Long userId) {
        return feedbackRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(f -> modelMapper.map(f, FeedbackDTO.class))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Terminate a feedback entry from the registry", operationId = "deleteFeedback")
    public void deleteFeedback(@PathVariable Long id) {
        feedbackRepository.deleteById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modify status or add administrative reply to feedback", operationId = "updateFeedback")
    public ResponseEntity<FeedbackDTO> updateFeedback(@PathVariable Long id, @RequestBody FeedbackDTO feedbackDTO) {
        return feedbackRepository.findById(id)
                .map(f -> {
                    f.setStatus(feedbackDTO.getStatus());
                    f.setAdminReply(feedbackDTO.getAdminReply());
                    Feedback saved = feedbackRepository.save(f);
                    return ResponseEntity.ok(modelMapper.map(saved, FeedbackDTO.class));
                }).orElse(ResponseEntity.notFound().build());
    }
}
