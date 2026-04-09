package com.example.education_library;

import com.example.education_library.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AppProperties.class)
public class EducationLibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(EducationLibraryApplication.class, args);
	}

}
