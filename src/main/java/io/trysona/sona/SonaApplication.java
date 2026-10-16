package io.trysona.sona;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SonaApplication {

    public static void main(String[] args) {
        SpringApplication.run(SonaApplication.class, args);
    }
}
