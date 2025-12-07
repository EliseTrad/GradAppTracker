package com.gradapptracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring Boot backend application.
 * <p>
 * This application provides REST API endpoints for managing graduate school
 * applications, documents, and user authentication using JWT tokens.
 * <p>
 * The backend runs on port 8080 by default and requires a PostgreSQL database.
 * See application.properties for configuration options.
 */
@SpringBootApplication
public class GradAppTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradAppTrackerApplication.class, args);
    }

}
