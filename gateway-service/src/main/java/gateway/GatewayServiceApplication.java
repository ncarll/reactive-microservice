package gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import java.net.Inet4Address;
import java.net.UnknownHostException;

@EnableDiscoveryClient
@SpringBootApplication
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    public RouteLocator accountServiceRoutes(final RouteLocatorBuilder builder) {
        return builder.routes()
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