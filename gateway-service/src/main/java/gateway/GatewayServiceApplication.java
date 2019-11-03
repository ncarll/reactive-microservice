package gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import java.net.Inet4Address;
import java.net.UnknownHostException;

/**
 * Create a unified front for the microservices
 *
 * @author ncarll
 * @implNote This service runs at :8080 while the other services take random port assignments (simulating different
 * hostnames/IP, different data centers).  Service differentiation is performed using path prefixing.
 */
@EnableDiscoveryClient
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    /**
     * Register routes with Java config.  This can be easily done in YAML instead for ease of production
     *
     * @param builder Route builder
     * @return RouteLocator bean
     */
    @Bean
    public RouteLocator serviceRoutes(final RouteLocatorBuilder builder) {
        return builder.routes()
                /*
                 * Create a route for /account
                 * - all requests starting with /account will go the account service
                 * - URL rewriting can be used to include version numbers, etc.
                 * - Request and response headers can also be added to aid routing and processing
                 */
                .route(predicateSpec -> predicateSpec
                        .path("/account/**")
                        .filters(f -> {
                            f.addResponseHeader("Service", "account-service");
                            try {
                                f.addResponseHeader("Gateway", Inet4Address.getLocalHost().getCanonicalHostName());
                            } catch (final UnknownHostException e) {
                                // intentional
                            }
                            return f;
                        })
                        .uri("lb://account-service"))
                .route(predicateSpec -> predicateSpec
                        .path("/profile/**")
                        .filters(f -> {
                            f.addResponseHeader("Service", "profile-service");
                            try {
                                f.addResponseHeader("Gateway", Inet4Address.getLocalHost().getCanonicalHostName());
                            } catch (final UnknownHostException e) {
                                // intentional
                            }
                            return f;
                        })
                        .uri("lb://profile-service"))
                .build();
    }
}