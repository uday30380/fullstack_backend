package com.example.education_library;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import com.example.education_library.model.Resource;
import com.example.education_library.model.User;
import com.example.education_library.repository.ResourceRepository;
import com.example.education_library.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.mail.host=smtp.gmail.com",
		"spring.mail.username=test-sender@educate.local",
		"spring.mail.password=test-app-password",
		"app.mail.from=test-sender@educate.local",
		"app.mail.admin-email=admin@educate.local"
})
@AutoConfigureMockMvc
@Transactional
class EducationLibraryApplicationTests {

	@TestConfiguration
	static class AsyncTestConfiguration {
		@Bean(name = "taskExecutor")
		TaskExecutor taskExecutor() {
			return new SyncTaskExecutor();
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ResourceRepository resourceRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@MockBean
	private JavaMailSender mailSender;

	@BeforeEach
	void setUpMailSender() {
		stubMailSender();
	}

	@Test
	void contextLoads() {
	}

	@Test
	void healthEndpointIsPublic() throws Exception {
		mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(content().string("Backend is running successfully."));
	}

	@Test
	void swaggerEndpointsArePublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.paths").exists());

		mockMvc.perform(get("/swagger-ui/index.html"))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Swagger UI")));
	}

	@Test
	void adminUserEndpointsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/auth/users"))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminUserEndpointsSupportListingUpdatingAndProfileLookup() throws Exception {
		User faculty = new User();
		faculty.setName("Faculty Runtime");
		faculty.setEmail("faculty.runtime@edulib.com");
		faculty.setPassword("test-password");
		faculty.setRole("Faculty");
		faculty.setStatus("Pending");
		faculty.setFacultyPin("654321");
		faculty = userRepository.save(faculty);

		String token = loginAsAdmin();

		mockMvc.perform(get("/api/auth/users")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].email", hasItem("faculty.runtime@edulib.com")));

		mockMvc.perform(put("/api/auth/users/{id}", faculty.getId())
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Faculty Runtime Updated",
								"status", "Active"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Faculty Runtime Updated"))
				.andExpect(jsonPath("$.status").value("Active"));

		mockMvc.perform(get("/api/auth/profile/{email}", "faculty.runtime@edulib.com")
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.facultyPin").value("654321"));
	}

	@Test
	void publicInquirySubmissionWorksWithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/inquiries")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Runtime Check",
								"email", "runtime@example.com",
								"subject", "other",
								"message", "Public inquiry submission check"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.email").value("runtime@example.com"));
	}

	@Test
	void generalFeedbackSubmissionWorksWithoutResourceId() throws Exception {
		String token = loginAsAdmin();

		mockMvc.perform(post("/api/feedback")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"userId", getAdminUser().getId(),
								"userEmail", "admin@edulib.com",
								"resourceTitle", "General Feedback",
								"content", "Runtime feedback submission check",
								"rating", 5,
								"status", "Pending"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.resourceTitle").value("General Feedback"));
	}

	@Test
	void adminResourceJsonUpdateEndpointAcceptsJsonPayloads() throws Exception {
		String token = loginAsAdmin();

		Resource resource = new Resource();
		resource.setTitle("Runtime Resource");
		resource.setSubject("Testing");
		resource.setDepartment("QA");
		resource.setType("Notes");
		resource.setDescription("Created by integration test");
		resource.setUploader("admin@edulib.com");
		resource.setStatus("Active");
		resource = resourceRepository.save(resource);

		mockMvc.perform(put("/api/admin/resources/{id}", resource.getId())
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("status", "Suspended"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("Suspended"));
	}

	@Test
	void registerLoginProfileUpdateAndDownloadSendEmailsWhenMailIsConfigured() throws Exception {
		stubMailSender();

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", "student.mail@edulib.com",
								"name", "Mail Student",
								"password", "Test@123456",
								"role", "Student",
								"dept", "QA",
								"idNumber", "STU-001"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());

		verify(mailSender, times(1)).send(any(MimeMessage.class));

		stubMailSender();

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"email", "student.mail@edulib.com",
								"password", "Test@123456"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString());

		verify(mailSender, times(1)).send(any(MimeMessage.class));

		User student = userRepository.findByEmailIgnoreCase("student.mail@edulib.com").orElseThrow();
		String token = login(student.getEmail(), "Test@123456");

		stubMailSender();

		mockMvc.perform(put("/api/users/{id}", student.getId())
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"name", "Mail Student Updated",
								"dept", "Engineering",
								"idNumber", "STU-001"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Mail Student Updated"));

		verify(mailSender, times(1)).send(any(MimeMessage.class));

		Resource resource = new Resource();
		resource.setTitle("Download Runtime Resource");
		resource.setSubject("Testing");
		resource.setDepartment("QA");
		resource.setType("Notes");
		resource.setDescription("Created for download mail verification");
		resource.setUploader("admin@edulib.com");
		resource.setStatus("Active");
		resource.setIsGlobal(true);
		resource = resourceRepository.save(resource);

		stubMailSender();

		mockMvc.perform(post("/api/resources/action")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of(
								"userId", student.getId(),
								"resourceId", resource.getId(),
								"resourceTitle", resource.getTitle(),
								"actionType", "DOWNLOAD"
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.actionType").value("DOWNLOAD"));

		verify(mailSender, times(1)).send(any(MimeMessage.class));
	}

	@Test
	void globalResourcesAreVisibleToStudentGuestAndFeaturedEndpoints() throws Exception {
		Resource resource = new Resource();
		resource.setTitle("Global Runtime Resource");
		resource.setSubject("Testing");
		resource.setDepartment("QA");
		resource.setType("Notes");
		resource.setDescription("Visible to every panel");
		resource.setUploader("admin@edulib.com");
		resource.setStatus("Active");
		resource.setIsGlobal(true);
		resource.setIsFeatured(true);
		resource.setFeaturedOrder(1);
		resourceRepository.save(resource);

		mockMvc.perform(get("/api/resources")
						.param("role", "Student"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("Global Runtime Resource")));

		mockMvc.perform(get("/api/resources")
						.param("role", "guest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("Global Runtime Resource")));

		mockMvc.perform(get("/api/resources/featured")
						.param("role", "Student"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].title", hasItem("Global Runtime Resource")));
	}

	private String loginAsAdmin() throws Exception {
		ensureAdminUser();

		String response = loginResponse(Map.of(
				"email", "admin@edulib.com",
				"password", "Test@123456",
				"secretCode", "ADMIN2026"
		));

		JsonNode body = objectMapper.readTree(response);
		return body.get("token").asText();
	}

	private String login(String email, String password) throws Exception {
		String response = loginResponse(Map.of(
				"email", email,
				"password", password
		));

		JsonNode body = objectMapper.readTree(response);
		return body.get("token").asText();
	}

	private String loginResponse(Map<String, String> request) throws Exception {
		return mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private User getAdminUser() {
		return ensureAdminUser();
	}

	private String bearer(String token) {
		return "Bearer " + token;
	}

	private User ensureAdminUser() {
		return userRepository.findByEmailIgnoreCase("admin@edulib.com")
				.orElseGet(() -> {
					User admin = new User();
					admin.setName("System Administrator");
					admin.setEmail("admin@edulib.com");
					admin.setPassword(passwordEncoder.encode("Test@123456"));
					admin.setRole("Admin");
					admin.setDept("Administration");
					admin.setIdNumber("ADMIN-001");
					admin.setStatus("Active");
					return userRepository.save(admin);
				});
	}

	private void stubMailSender() {
		reset(mailSender);
		when(mailSender.createMimeMessage())
				.thenAnswer(invocation -> new MimeMessage(Session.getInstance(new Properties())));
	}
}
