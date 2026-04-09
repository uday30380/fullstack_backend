package com.example.education_library.controller;

import com.example.education_library.dto.InquiryDTO;
import com.example.education_library.model.Inquiry;
import com.example.education_library.repository.InquiryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inquiries")
@Tag(name = "Inquiry Management", description = "Operations related to scholarly inquiries")
public class InquiryController {

    @Autowired
    private InquiryRepository repository;

    @Autowired
    private com.example.education_library.service.EmailService emailService;

    @Autowired
    private com.example.education_library.config.AppProperties appProperties;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping
    @Operation(summary = "Get all inquiries ordered by date", operationId = "getAllInquiries")
    public List<InquiryDTO> getAllInquiries() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(inquiry -> modelMapper.map(inquiry, InquiryDTO.class))
                .collect(Collectors.toList());
    }

    @PostMapping
    @Operation(summary = "Submit a new scholarly inquiry", operationId = "submitInquiry")
    public InquiryDTO submitInquiry(@RequestBody InquiryDTO inquiryDTO) {
        Inquiry inquiry = modelMapper.map(inquiryDTO, Inquiry.class);
        System.out.println("New scholarly inquiry dispatch received from: " + inquiry.getEmail());
        Inquiry savedInquiry = repository.save(inquiry);
        
        // Dispatch notifications
        try {
            emailService.sendInquiryNotification(
                appProperties.getMail().getAdminEmail(),
                savedInquiry.getName(),
                savedInquiry.getEmail(),
                savedInquiry.getSubject(),
                savedInquiry.getMessage()
            );
            emailService.sendInquiryConfirmation(savedInquiry.getEmail(), savedInquiry.getName());
        } catch (Exception e) {
            System.err.println("INQUIRY DISPATCH ERROR: " + e.getMessage());
        }
        
        return modelMapper.map(savedInquiry, InquiryDTO.class);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an inquiry by ID", operationId = "deleteInquiry")
    public ResponseEntity<Void> deleteInquiry(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
