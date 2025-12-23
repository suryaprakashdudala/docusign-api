package com.docusign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing(auditorAwareRef = "auditorAware")
public class DocusignApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocusignApiApplication.class, args);
	}

}
