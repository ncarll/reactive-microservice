package com.enrsx.reactivemicroservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class AccountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }
}

@Log4j2
@Component
@RequiredArgsConstructor
class SampleDataInitializer {

    private final AccountRepository accountRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {

        final var saved = Flux
                .just("Josh", "Paul", "Rando", "Stephan", "Meghan")
                .map(name -> new Account(null, name))
                .flatMap(accountRepository::save);

        accountRepository
                .deleteAll()
                .thenMany(saved)
                .thenMany(accountRepository.findAll())
                .subscribe(logger::info);
    }
}

@Document
@Data
@AllArgsConstructor
@RequiredArgsConstructor
class Account {

    @Id
    private String id;
    private String name;
}

interface AccountRepository extends ReactiveCrudRepository<Account, String> {

}

@RestController
@RequiredArgsConstructor
class AccountController {

    private final AccountRepository accountRepository;
    private final IntervalMessageProducer producer;

    @GetMapping("/accounts")
    Flux<Account> accountPublisher() {
        return accountRepository.findAll();
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE, value = "/sse/{name}")
    public Flux<String> ssePublisher(@PathVariable final String name) {
        return producer.produce(name);
    }
}

@Component
class IntervalMessageProducer {

    Flux<String> produce(final String name) {
        return Flux.fromStream(Stream.generate(() -> "Hello " + name + " @ " + Instant.now()))
                .delayElements(Duration.ofSeconds(1));
    }
}