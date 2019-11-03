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

/**
 * Application entrypoint
 *
 * @author ncarll
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ProfileServiceApplication {

    /**
     * Main method
     *
     * @param args Program args
     */
    public static void main(final String[] args) {
        SpringApplication.run(ProfileServiceApplication.class, args);
    }
}

/**
 * HTTP controller
 */
@Slf4j
@RestController
@RequiredArgsConstructor
class ProfileController {

    private final ServiceResolver serviceResolver;

    /**
     * Similar to /account/sse/{name}, create SSE stream of string data
     *
     * @param name Name to add to string data
     * @return Long-running one-way communication over HTTP with client browser
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/profile/sse/{name}")
    public Flux<String> nameFlux(@PathVariable final String name) {
        logger.info("Requesting flux for {}", name);

        /*
         * Resolve the service by name to get a RSocketRequester,
         * Requester opens a TCP connection with the service (direct service-to-service communication),
         * Route corresponds to the @MessageMapping in account-service
         * Account-service requires a String {name} argument, so send it
         * Receive Strings as they are created by the remote service, and write them to the SSE connection
         */
        final var requesterMono = serviceResolver.getService(ServiceRegistry.ACCOUNT_SERVICE);
        return requesterMono.flatMapMany(requester ->
                requester.route("account")
                        .data(name)
                        .retrieveFlux(String.class));
    }
}

/**
 * Service resolution by name using the DiscoveryClient
 */
@Slf4j
@Service
@RequiredArgsConstructor
class ServiceResolver {

    private final DiscoveryClient discoveryClient;

    /**
     * Get a service by name
     *
     * @param serviceName The service name
     * @return Single publisher of RSocketRequester
     */
    Mono<RSocketRequester> getService(final String serviceName) {

        // Ask the service registry for a collection of service instances by name
        final var accountServiceInstances = discoveryClient.getInstances(serviceName);
        if (CollectionUtils.isEmpty(accountServiceInstances)) {
            throw new RuntimeException("Could not find service to connect to");
        }

        // Load balancing strategy = "take the first one"
        final var accountService = accountServiceInstances.get(0);

        /*
         * If a service instance was found, create a new RSocketRequester mono
         * - connect by TCP (can use WebSocket)
         * - get remote hostname from service instance
         * - get the RSocket port for the service instance from metadata (added in account-service application.yaml)
         * - register consumers for a few standard events: new subscribe, publisher terminated, error
         */
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