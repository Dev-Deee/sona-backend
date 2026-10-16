package io.trysona.sona;

import org.springframework.boot.SpringApplication;

/**
 * Runs the app against a throwaway Testcontainers PostgreSQL: {@code ./mvnw spring-boot:test-run}.
 */
public class TestSonaApplication {

    public static void main(String[] args) {
        SpringApplication.from(SonaApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
