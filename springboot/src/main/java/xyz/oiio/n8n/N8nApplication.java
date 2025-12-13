package xyz.oiio.n8n;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class N8nApplication {

    public static void main(String[] args) {
        SpringApplication.run(N8nApplication.class, args);
    }
}