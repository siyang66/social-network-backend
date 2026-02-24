package com.meet5.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Application Class
 * Social Network Backend System
 * 
 * Senior Backend Assignment
 */
@SpringBootApplication
@EnableScheduling
public class SocialNetworkApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(SocialNetworkApplication.class, args);
    }
}
