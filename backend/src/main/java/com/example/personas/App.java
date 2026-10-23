package com.example.personas;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        Dotenv.configure().ignoreIfMissing().load().entries().forEach(entry -> {
            if (System.getenv(entry.getKey()) == null && System.getProperty(entry.getKey()) == null)
                System.setProperty(entry.getKey(), entry.getValue());
        });
        SpringApplication.run(App.class, args);
    }
}
