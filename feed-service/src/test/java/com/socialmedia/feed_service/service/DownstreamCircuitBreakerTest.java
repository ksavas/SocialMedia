package com.socialmedia.feed_service.service;

import com.socialmedia.feed_service.config.ResilienceConfig;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DownstreamCircuitBreakerTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicInteger requests = new AtomicInteger();
    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        circuitBreaker = new ResilienceConfig().circuitBreakerRegistry().circuitBreaker("user-service");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void circuitOpensAfterRepeatedServerErrorsAndFailsFastWithoutCallingService() {
        respondWith(500);
        UserServiceClient client = new UserServiceClient(baseUrl, circuitBreaker);

        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> client.getFollowerCount("author")).isInstanceOf(HttpServerErrorException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThatThrownBy(() -> client.getFollowerCount("author")).isInstanceOf(CallNotPermittedException.class);
        assertThat(requests.get()).isEqualTo(5);
    }

    @Test
    void clientErrorsDoNotOpenCircuit() {
        respondWith(404);
        UserServiceClient client = new UserServiceClient(baseUrl, circuitBreaker);

        for (int call = 0; call < 10; call++) {
            assertThatThrownBy(() -> client.getFollowerCount("author")).isInstanceOf(HttpClientErrorException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    void connectionFailuresOpenCircuit() {
        server.stop(0);
        PostServiceClient client = new PostServiceClient(baseUrl, circuitBreaker);

        for (int call = 0; call < 5; call++) {
            assertThatThrownBy(() -> client.markForPull("p1")).isInstanceOf(ResourceAccessException.class);
        }

        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    private void respondWith(int status) {
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
    }
}
