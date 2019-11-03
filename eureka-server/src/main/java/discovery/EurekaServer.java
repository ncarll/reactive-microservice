package discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Run the Eureka server
 *
 * @author ncarll
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServer {

    /**
     * Main method
     *
     * @param args Program args
     */
    public static void main(final String[] args) {
        SpringApplication.run(EurekaServer.class, args);
    }
}