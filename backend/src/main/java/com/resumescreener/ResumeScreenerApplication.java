package com.resumescreener;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ResumeScreenerApplication {

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
            .directory("..")
            .filename(".env")
            .ignoreIfMissing()
            .load();

        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        SpringApplication.run(ResumeScreenerApplication.class, args);
    }
}
