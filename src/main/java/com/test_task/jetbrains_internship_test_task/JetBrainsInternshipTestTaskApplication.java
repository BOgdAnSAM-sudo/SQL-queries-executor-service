package com.test_task.jetbrains_internship_test_task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class JetBrainsInternshipTestTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetBrainsInternshipTestTaskApplication.class, args);
    }

}
