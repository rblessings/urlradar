package com.github.rblessings;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    public GenericContainer<?> kafkaContainer() {
        GenericContainer<?> container = new GenericContainer<>("apache/kafka-native:latest")
                .withExposedPorts(9092)
                .waitingFor(Wait.forListeningPort()); // Wait for the container to be ready
        container.start();
        return container;
    }

    @Bean
    @ServiceConnection
    protected MongoDBContainer mongoDbContainer() {
        return new MongoDBContainer(DockerImageName.parse("mongo:latest"))
                .withExposedPorts(27017);
    }

    @Bean
    @ServiceConnection(name = "redis")
    protected GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:latest"))
                .withExposedPorts(6379);
    }

    @Bean
    public GenericContainer<?> authorizationServerContainer() {
        GenericContainer<?> container = new GenericContainer<>(
                "rblessings/oauth2-oidc-jwt-auth-server:latest")
                .withEnv("spring.profiles.active", "dev")
                .withExposedPorts(9000)
                .waitingFor(Wait.forListeningPort()); // Wait for the container to be ready
        container.start();
        return container;
    }

    @Bean
    public Integer authorizationServerContainerPort(GenericContainer<?> authorizationServerContainer) {
        var port = authorizationServerContainer.getMappedPort(9000);
        System.setProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://localhost:%d".formatted(port));
        return port;
    }
}
