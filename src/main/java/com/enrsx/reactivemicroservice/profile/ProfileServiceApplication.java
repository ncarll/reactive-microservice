package com.enrsx.reactivemicroservice.profile;

import com.enrsx.reactivemicroservice.registry.ServiceRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@EnableDiscoveryClient
@PropertySource("classpath:profile.properties")
@SpringBootApplication(scanBasePackages = "com.enrsx.reactivemicroservice.profile", exclude =
        {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoReactiveAutoConfiguration.class})
public class ProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
    }
}

@RestController
@RequiredArgsConstructor
class ProfileController {

    private final ServiceResolver serviceResolver;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/account/sse/{name}")
    public Flux<String> nameFlux(@PathVariable final String name) {
        final var requester = serviceResolver.getService(ServiceRegistry.ACCOUNT_SERVICE);
        return requester.route("account")
                .data(name)
                .retrieveFlux(String.class);
    }
}

@Service
@RequiredArgsConstructor
class ServiceResolver {

    private final DiscoveryClient discoveryClient;

    RSocketRequester getService(final String serviceName) {
        final var accountServiceInstances = discoveryClient.getInstances(serviceName);
        if (CollectionUtils.isEmpty(accountServiceInstances)) {
            throw new RuntimeException("Could not find service to connect to");
        }

        final var accountService = accountServiceInstances.get(0);

        return RSocketRequester.builder()
                .connectTcp(
                        accountService.getHost(),
                        Integer.parseInt(accountService.getMetadata().get("rsocket-port"))
                ).block();
    }
}