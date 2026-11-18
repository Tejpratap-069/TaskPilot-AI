package com.taskpilot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TaskPilotApplication {
    public static void main(String[] args) {
        SpringApplication.run(TaskPilotApplication.class, args);
    }
}
