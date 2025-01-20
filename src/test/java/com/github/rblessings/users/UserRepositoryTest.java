package com.github.rblessings.users;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles({"dev"})
@DataMongoTest
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static final MongoDBContainer MONGODB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:latest"))
                    .withExposedPorts(27017)
                    .waitingFor(Wait.forListeningPort())
                    .waitingFor(Wait.forSuccessfulCommand("mongosh --eval \"db.adminCommand('ping').ok\""));

    @Autowired
    UserRepositoryTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> String.format(
                "mongodb://localhost:%d/urlradar", MONGODB_CONTAINER.getFirstMappedPort()));
    }

    private final UserRepository userRepository;

    @Test
    void shouldPersistUser_whenValidDetailsAreProvided() {
        // Arrange: Create a valid UserEntity to test persistence behavior with correct input data.
        UserEntity userToSave = new UserEntity(null, "Ranzy", "Blessings",
                "blessingsihembi@gmail.com", "secret", null);

        // Act: Persist the user and capture the result in a Mono.
        Mono<UserEntity> savedUserMono = userRepository.save(userToSave);

        // Assert: Ensure the saved user matches the expected values, with non-null ID and version.
        StepVerifier.create(savedUserMono)
                .expectNextMatches(savedUser -> {
                    assertNotNull(savedUser.id(), "User ID must be generated.");
                    assertEquals("Ranzy", savedUser.firstName(), "First name mismatch.");
                    assertEquals("Blessings", savedUser.lastName(), "Last name mismatch.");
                    assertEquals("blessingsihembi@gmail.com", savedUser.email(), "Email mismatch.");
                    assertNotNull(savedUser.version(), "Version should be auto-generated.");
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void shouldThrowOptimisticLockingFailure_whenConcurrentUpdatesAttemptToModifySameEntity() {
        // Arrange: Create an initial UserEntity to test optimistic locking behavior.
        UserEntity initialUserEntity = new UserEntity("1", "John", "Doe",
                "john.doe@example.com", "newPassword", null);

        Mono<UserEntity> savedEntityMono = userRepository.save(initialUserEntity);

        // Act: Simulate a concurrent update with an outdated version to trigger optimistic locking failure.
        savedEntityMono
                .flatMap(savedEntity -> {
                    UserEntity staleUserEntity = new UserEntity(savedEntity.id(), "John", "Doe",
                            "john.doe@example.com", "updatedPassword", savedEntity.version() - 1);
                    return userRepository.save(staleUserEntity);
                })
                // Assert: Ensure the optimistic locking exception is thrown on conflicting save attempt.
                .as(StepVerifier::create)
                .expectError(OptimisticLockingFailureException.class)
                .verify();
    }
}
