package profile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@EnableDiscoveryClient
@SpringBootApplication
public class ProfileServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
    }
}

@Slf4j
@RestController
@RequiredArgsConstructor
class ProfileController {

    private final ServiceResolver serviceResolver;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/profile/sse/{name}")
    public Flux<String> nameFlux(@PathVariable final String name) {
        logger.info("Requesting flux for {}", name);
        final var requesterMono = serviceResolver.getService(ServiceRegistry.ACCOUNT_SERVICE);
        return requesterMono.flatMapMany(requester ->
                requester.route("account")
                        .data(name)
                        .retrieveFlux(String.class));
    }
}

@Slf4j
@Service
@RequiredArgsConstructor
class ServiceResolver {

    private final DiscoveryClient discoveryClient;

    Mono<RSocketRequester> getService(final String serviceName) {
        final var accountServiceInstances = discoveryClient.getInstances(serviceName);
        if (CollectionUtils.isEmpty(accountServiceInstances)) {
            throw new RuntimeException("Could not find service to connect to");
        }

        final var accountService = accountServiceInstances.get(0);

        return RSocketRequester.builder()
                .connectTcp(
                        accountService.getHost(),
                        Integer.parseInt(accountService.getMetadata().get("rsocket-port")))
                .cache()
                .doOnSubscribe(sub -> logger.info("RSocket connection established to {}", serviceName))
                .doOnSuccess(complete -> logger.info("RSocket connection to {} completed successfully", serviceName))
                .doOnError(ex -> logger.info("Exception communicating with {} {}", serviceName, ex));
    }
}