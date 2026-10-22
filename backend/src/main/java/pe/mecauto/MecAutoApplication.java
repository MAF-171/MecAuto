package pe.mecauto;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MecAutoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MecAutoApplication.class, args);
    }
}
