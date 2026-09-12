package com.socialmedia.feed_service.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridFeedHttpContractTest {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void userClientReadsCountWithoutFetchingFollowerListAndReadsFollowingIds() {
        server.createContext("/api/internal/users/author/follower-count", exchange -> {
            byte[] body = "10000".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/api/internal/users/reader/following-ids", exchange -> {
            byte[] body = "[\"author\"]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        UserServiceClient client = new UserServiceClient(baseUrl);

        assertThat(client.getFollowerCount("author")).isEqualTo(10000);
        assertThat(client.getFollowingIds("reader")).containsExactly("author");
    }

    @Test
    void postClientUsesPutAndDeserializesPullFeedDates() {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> request = new AtomicReference<>();
        server.createContext("/api/internal/posts/p1/fanout-on-read", exchange -> {
            method.set(exchange.getRequestMethod());
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        server.createContext("/api/internal/posts/pull-feed", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "[{\"id\":\"p1\",\"authorId\":\"author\",\"createdAt\":\"2026-10-17T10:00:00Z\"}]".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        PostServiceClient client = new PostServiceClient(baseUrl);

        client.markForPull("p1");
        var posts = client.getPullPosts(List.of("author"), 1000);

        assertThat(method.get()).isEqualTo("PUT");
        assertThat(request.get()).contains("\"authorIds\":[\"author\"]", "\"limit\":1000");
        assertThat(posts).hasSize(1);
        assertThat(posts.getFirst().createdAt()).isEqualTo(Instant.parse("2026-10-17T10:00:00Z"));
    }

    @Test
    void failedPullRegistrationPropagatesHttpFailure() {
        server.createContext("/api/internal/posts/p1/fanout-on-read", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> new PostServiceClient(baseUrl).markForPull("p1"))
                .isInstanceOf(HttpServerErrorException.class);
    }
}
