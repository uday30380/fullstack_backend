package com.example.education_library.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.education_library.config.AppProperties;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AppProperties appProperties;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email with empty recipient for subject '{}'", subject);
            return;
        }

        if (!isMailConfigured()) {
            log.warn("Skipping email '{}' because SMTP is not fully configured. Configure spring.mail.host plus valid credentials before expecting delivery.", subject);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(resolveFromAddress());
            helper.setTo(to.trim());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}: {}", to, subject);
        } catch (MessagingException | MailException e) {
            log.warn("Failed to send email to {}: {}. This may be due to mail configuration issues.", to, subject, e);
        }
    }

    private boolean isMailConfigured() {
        if (mailHost == null || mailHost.isBlank()) {
            return false;
        }
        if (!smtpAuth) {
            return true;
        }
        return mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    private String resolveFromAddress() {
        String configuredFrom = appProperties.getMail().getFrom();
        if (configuredFrom != null && !configuredFrom.isBlank()) {
            return configuredFrom.trim();
        }
        if (mailUsername != null && !mailUsername.isBlank()) {
            return mailUsername.trim();
        }
        return "no-reply@educate.local";
    }

    private String buildHtmlTemplate(String title, String... paragraphs) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 40px; border: 1px solid #e5e7eb; border-radius: 12px; background-color: #ffffff;\">");
        sb.append("<div style=\"text-align: center; margin-bottom: 30px;\">");
        sb.append("<img src=\"https://img.icons8.com/color/96/000000/graduation-cap.png\" alt=\"Educate Logo\" style=\"width: 72px; height: 72px; margin-bottom: 16px;\" />");
        sb.append("<div style=\"font-size: 56px; font-weight: 900; letter-spacing: -2px;\">");
        sb.append("<span style=\"color: #111827;\">Educate</span><span style=\"color: #f97316;\">.</span>");
        sb.append("</div></div>");
        sb.append("<h2 style=\"color: #111827; font-size: 24px; font-weight: bold; margin-bottom: 24px; text-align: center;\">").append(title).append("</h2>");

        for (String p : paragraphs) {
            sb.append("<p style=\"color: #4b5563; font-size: 16px; line-height: 1.6; margin-bottom: 24px;\">").append(p).append("</p>");
        }

        sb.append("<hr style=\"border: none; border-top: 1px solid #e5e7eb; margin-bottom: 24px;\">");
        sb.append("<p style=\"color: #9ca3af; font-size: 14px; text-align: center;\">Best regards,<br><strong>The Educate. Administration Team</strong></p>");
        sb.append("</div>");
        return sb.toString();
    }

    private String buildFrontendUrl(String path) {
        return UriComponentsBuilder.fromUriString(appProperties.getFrontendUrl())
                .path(path)
                .build()
                .toUriString();
    }

    private String buildPasswordResetUrl(String toEmail, String otp) {
        return UriComponentsBuilder.fromUriString(appProperties.getFrontendUrl())
                .path("/reset-password")
                .queryParam("token", otp)
                .queryParam("email", toEmail)
                .build()
                .encode()
                .toUriString();
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) return "Scholar";
        // Handle potentially long or corrupt names from the image issue
        String cleanName = name.trim();
        if (cleanName.length() > 30) {
            cleanName = cleanName.substring(0, 27) + "...";
        }
        
        // Simple Title Case
        if (cleanName.length() > 0) {
            return Character.toUpperCase(cleanName.charAt(0)) + cleanName.substring(1);
        }
        return "Scholar";
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String name) {
        String formattedName = formatName(name);
        String body = buildHtmlTemplate("Welcome to Educate.",
            "Dear " + formattedName + ",",
            "Welcome to Educate., the elite nexus for academic collaboration. Your account has been successfully established and synchronized with our core repositories.",
            "You can now access our global repository of multidisciplinary archives, bookmark your favorite resources, and collaborate with innovators worldwide.",
            "Your institutional clearance is active and your academic node is fully operational.",
            "<div style=\"text-align: center;\"><a href=\"" + buildFrontendUrl("/home") + "\" style=\"display: inline-block; background-color: #f97316; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(249, 115, 22, 0.3);\">Explore the Archives</a></div>"
        );
        sendHtmlEmail(toEmail, "Welcome to Educate. - Your Academic Journey Begins", body);
    }

    @Async
    public void sendInquiryNotification(String adminEmail, String name, String email, String subject, String message) {
        String body = buildHtmlTemplate("New Scholarly Inquiry",
            "Institutional Alert: A new inquiry has been dispatched via the contact portal.",
            "<strong>Sender:</strong> " + name + " (" + email + ")",
            "<strong>Subject:</strong> " + subject,
            "<strong>Message:</strong>",
            "<div style=\"background-color: #f3f4f6; padding: 20px; border-radius: 8px; font-style: italic;\">" + message + "</div>",
            "<div style=\"text-align: center; margin-top: 20px;\"><a href=\"" + buildFrontendUrl("/admin/dashboard") + "\" style=\"display: inline-block; background-color: #111827; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 6px; font-size: 14px;\">Review in Portal</a></div>"
        );
        sendHtmlEmail(adminEmail, "ACTION REQUIRED: New Inquiry - " + subject, body);
    }

    @Async
    public void sendInquiryConfirmation(String userEmail, String name) {
        String body = buildHtmlTemplate("Inquiry Logged",
            "Hello " + formatName(name) + ",",
            "Your scholarly inquiry has been successfully dispatched to the central administration.",
            "An academic coordinator will review your request and articulate a response within the standard institutional timeframe.",
            "Thank you for your patience and contribution to the nexus."
        );
        sendHtmlEmail(userEmail, "Inquiry Received - Educate.", body);
    }

    @Async
    public void sendFacultyApplicationEmail(String toEmail, String name) {
        String body = buildHtmlTemplate("Application Received",
            "Dear Dr. " + formatName(name) + ",",
            "Thank you for your interest in joining the academic faculty on Educate. Your request for institutional privileges has been successfully logged.",
            "Your account is currently in a <strong>PENDING</strong> state. Our administrative team will review your credentials and scholarly background shortly.",
            "You will receive an automated notification as soon as the status of your application is updated.",
            "In the meantime, you can still explore the public archives as a student/guest node."
        );
        sendHtmlEmail(toEmail, "Institutional Application Logged - Educate.", body);
    }

    @Async
    public void notifyAdminOfNewFaculty(String facultyName, String facultyEmail) {
        String body = buildHtmlTemplate("New Faculty Node Pending",
            "Institutional Alert:",
            "A new scholar, <strong>" + facultyName + "</strong> (" + facultyEmail + "), has applied for Faculty privileges.",
            "Please review their academic credentials in the Administration Panel to approve or deny institutional access.",
            "<div style=\"text-align: center;\"><a href=\"" + buildFrontendUrl("/admin/users") + "\" style=\"display: inline-block; background-color: #f97316; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(249, 115, 22, 0.3);\">Open Portal Registry</a></div>"
        );
        sendHtmlEmail(appProperties.getMail().getAdminEmail(), "ACTION REQUIRED: New Faculty Application - Educate.", body);
    }

    @Async
    public void sendLoginNotification(String toEmail, String name) {
        String body = buildHtmlTemplate("Terminal Access Granted",
            "Hello " + formatName(name) + ",",
            "A successful login has been executed for your academic account on Educate. All systems are synchronized.",
            "If you did not execute this login sequence, please initiate account lockdown procedures immediately."
        );
        sendHtmlEmail(toEmail, "New Login Detected - Educate.", body);
    }

    @Async
    public void sendProfileUpdateNotification(String toEmail, String name) {
        String body = buildHtmlTemplate("Profile Configured",
            "Hello " + formatName(name) + ",",
            "Your academic profile metadata on Educate. has been successfully synchronized and updated.",
            "All modifications have been securely stored in the institutional registry."
        );
        sendHtmlEmail(toEmail, "Profile Update Confirmation - Educate.", body);
    }

    @Async
    public void sendDownloadNotification(String toEmail, String resourceTitle) {
        String body = buildHtmlTemplate("Resource Retrieved",
            "Hello,",
            "This is an automated confirmation that you have successfully downloaded the document: <strong>\"" + resourceTitle + "\"</strong>.",
            "We hope this academic material greatly assists your collegiate objectives."
        );
        sendHtmlEmail(toEmail, "Download Successful - Educate.", body);
    }

    @Async
    public void sendSaveNotification(String toEmail, String resourceTitle) {
        String body = buildHtmlTemplate("Resource Appended to Vault",
            "Hello,",
            "Excellent choice! You have officially saved <strong>\"" + resourceTitle + "\"</strong> to your private academic vault.",
            "It will remain cataloged in your saved collection for immediate future retrieval."
        );
        sendHtmlEmail(toEmail, "Resource Saved - Educate.", body);
    }

    @Async
    public void sendReviewNotification(String toEmail, String resourceTitle) {
        String body = buildHtmlTemplate("Review Dispatched",
            "Hello,",
            "Your critical feedback on <strong>\"" + resourceTitle + "\"</strong> has been permanently logged in the public archive.",
            "Your input dramatically improves the pedagogical standard of our nexus. Thank you for contributing."
        );
        sendHtmlEmail(toEmail, "Review Captured - Educate.", body);
    }

    @Async
    public void notifyUploaderOfReview(String toEmail, String resourceTitle, String reviewer, int rating) {
        String body = buildHtmlTemplate("New Scholarly Appraisal",
            "Urgent Notification:",
            "Your uploaded asset <strong>\"" + resourceTitle + "\"</strong> has received a new pedagogical review from <strong>" + reviewer + "</strong>.",
            "Numerical Assessment: <strong style=\"color: #f97316;\">" + rating + " / 5 Stars</strong>",
            "Please review the feedback in your Faculty Management Panel to articulate a response or refine the material.",
            "<div style=\"text-align: center;\"><a href=\"" + buildFrontendUrl("/admin/resources") + "\" style=\"display: inline-block; background-color: #111827; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.1);\">Manage Resources</a></div>"
        );
        sendHtmlEmail(toEmail, "ACTION REQUIRED: New Review on Your Resource - Educate.", body);
    }

    @Async
    public void sendFacultyApprovalEmail(String toEmail, String name) {
        String body = buildHtmlTemplate("Faculty Node Activated",
            "Dear Dr. " + formatName(name) + ",",
            "We are pleased to inform you that your institutional application for Faculty privileges on Educate. has been officially <strong>APPROVED</strong>.",
            "Your academic identity has been synchronized with our core repositories. You now have full administrative access to upload scholarly resources, manage pedagogical materials, and interact with the student body.",
            "<div style=\"text-align: center;\"><a href=\"" + buildFrontendUrl("/login") + "\" style=\"display: inline-block; background-color: #f97316; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(249, 115, 22, 0.3);\">Login to Faculty Panel</a></div>",
            "Your contributions will dramatically improve the pedagogical standard of our global community."
        );
        sendHtmlEmail(toEmail, "Institutional Approval Granted - Educate.", body);
    }

    @Async
    public void sendFacultyDenialEmail(String toEmail, String name) {
        String body = buildHtmlTemplate("Application Update",
            "Hello " + formatName(name) + ",",
            "Your application for Faculty privileges on Educate. has been reviewed by the Central Administration. At this time, your request for elevated access has been <strong>DENIED</strong>.",
            "If you believe this was an error or would like to request further clarification regarding the institutional policy, please coordinate directly with your system administrator or the academic oversight board.",
            "Access to the student-level global archives remains available to you."
        );
        sendHtmlEmail(toEmail, "Institutional Access Update - Educate.", body);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String otp) {
        String resetLink = buildPasswordResetUrl(toEmail, otp);
        String body = buildHtmlTemplate("Password Recovery Node",
            "Hello,",
            "A password recovery node has been initialized for your academic account. Your secure 6-digit one-time password (OTP) is:",
            "<div style=\"background-color: #f3f4f6; border-radius: 8px; padding: 20px; text-align: center;\"><span style=\"font-size: 36px; font-weight: 800; color: #111827; letter-spacing: 8px;\">" + otp + "</span></div>",
            "You can enter this code on the recovery page, or simply click the button below to continue with your recovery flow:",
            "<div style=\"text-align: center;\"><a href=\"" + resetLink + "\" style=\"display: inline-block; background-color: #f97316; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(249, 115, 22, 0.3);\">Restore Access Now</a></div>",
            "This link actively expires in precisely 30 minutes for strict security purposes."
        );
        sendHtmlEmail(toEmail, "Password Recovery Code - Educate.", body);
    }

    @Async
    public void sendEmailVerificationCode(String toEmail, String otp) {
        String body = buildHtmlTemplate("Identity Verification Protocol",
            "Hello,",
            "To finalize your scholastic identity node on Educate., please use the following 6-digit verification code:",
            "<div style=\"background-color: #f3f4f6; border-radius: 8px; padding: 20px; text-align: center;\"><span style=\"font-size: 36px; font-weight: 800; color: #111827; letter-spacing: 8px;\">" + otp + "</span></div>",
            "This code is strictly required to verify your institutional email address and will expire in matching 30 minutes.",
            "If you did not initiate this registration sequence, please ignore this communication."
        );
        sendHtmlEmail(toEmail, "Identity Verification Code - Educate.", body);
    }
}
