package com.dvs.fluxdemo.web;

import com.dvs.fluxdemo.domain.Customer;
import com.dvs.fluxdemo.domain.CustomerRepository;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@RestController
public class CustomerController {

  private final CustomerRepository customerRepository;
  private final Sinks.Many<Customer> sink;

  public CustomerController(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
    sink = Sinks.many().multicast().onBackpressureBuffer();
  }

  @GetMapping("/flux")
  public Flux<Integer> flux() {
    return Flux.just(1,2,3,4,5).delayElements(Duration.ofSeconds(1)).log();
  }

  @GetMapping(value = "/fluxstream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
  public Flux<Integer> fluxstream() {
    return Flux.just(1,2,3,4,5).delayElements(Duration.ofSeconds(1)).log();
  }

  @GetMapping("/customer")
  public Flux<Customer> findAll() {
    return customerRepository.findAll().log();
  }

  @GetMapping("/customer/{id}")
  public Mono<Customer> findAll(@PathVariable Long id) {
    return customerRepository.findById(id).log();
  }

  @GetMapping(value = "/customer/sse")
  public Flux<ServerSentEvent<Customer>> findAllSSE() {
    return sink.asFlux().map(c -> ServerSentEvent.builder(c).build()).doOnCancel(() -> {
      sink.asFlux().blockLast();
    });
  }

  @PostMapping("/customer")
  public Mono<Customer> save() {
    return customerRepository.save(new Customer("gildong", "Hong")).doOnNext(c -> {
      sink.tryEmitNext(c);
    });
  }
}
