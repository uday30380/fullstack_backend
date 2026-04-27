package com.example.education_library.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.education_library.config.AppProperties;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter authFilter;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AppProperties appProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Explicitly permit all preflight requests
                .requestMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register", "/api/auth/forgot-password", "/api/auth/reset-password", "/api/auth/verify-email", "/api/auth/resend-verification").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/v3/api-docs/swagger-config"
                ).permitAll()
                .requestMatchers(HttpMethod.GET, "/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/resources", "/api/resources/featured", "/api/resources/files/**", "/api/resources/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/feedback/resource/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/inquiries").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/admin/dashboard/recent-users", "/api/admin/dashboard/recent-feedback").hasRole("ADMIN")
                .requestMatchers("/api/admin/resources/**", "/api/admin/dashboard/**").hasAnyRole("ADMIN", "FACULTY")
                .requestMatchers("/api/auth/users/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/auth/profile/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/auth/join-faculty").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/feedback").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/feedback/user/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/feedback").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/feedback/**", "/api/notifications/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/notifications").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/feedback/**", "/api/notifications/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/notifications/role/**").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/notifications").hasRole("ADMIN")
                .requestMatchers("/api/users/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    System.err.println("UNAUTHENTICATED ACCESS BLOCKED: " + request.getRequestURI());
                    response.setContentType("application/json");
                    response.setStatus(401);
                    response.getWriter().write("{\"status\": 401, \"message\": \"Authentication required for this node: Access pin or token missing.\" }");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    System.err.println("ACCESS DENIED (Unauthorized): " + request.getRequestURI());
                    response.setContentType("application/json");
                    response.setStatus(403);
                    response.getWriter().write("{\"status\": 403, \"message\": \"Clearance level insufficient: You do not have the required administrative role for this operation.\" }");
                })
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(){
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Combine configured origins with some defaults for safety
        List<String> allowedOrigins = new ArrayList<>(appProperties.getCors().getAllowedOrigins());
        
        // Add common variations of the Vercel URL if present in the base list
        List<String> variations = new ArrayList<>();
        for (String origin : allowedOrigins) {
            if (origin.endsWith("/")) {
                variations.add(origin.substring(0, origin.length() - 1));
            } else {
                variations.add(origin + "/");
            }
        }
        allowedOrigins.addAll(variations);
        
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
