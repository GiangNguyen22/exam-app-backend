package com.android.app.exam_app_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ExamAppBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamAppBackendApplication.class, args);
	}

}
