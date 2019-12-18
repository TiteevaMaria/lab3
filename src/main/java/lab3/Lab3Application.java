package lab3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication
@EnableWebSocket
public class Lab3Application
{
    public static void main(String[] args)
    {
        SpringApplication.run(Lab3Application.class, args);
    }
}
