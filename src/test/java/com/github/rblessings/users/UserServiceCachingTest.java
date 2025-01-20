package com.github.rblessings.users;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles({"dev"})
@SpringBootTest
@Testcontainers
class UserServiceCachingTest {

    @Container
    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS_CONTAINER =
            new GenericContainer<>(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(6379)
                    .waitingFor(Wait.forListeningPort())
                    .waitingFor(Wait.forSuccessfulCommand("redis-cli PING"));

    @DynamicPropertySource
    static void dynamicPropertySource(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", REDIS_CONTAINER::getFirstMappedPort);
    }

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CacheManager cacheManager;

    private UserEntity userEntity;

    @BeforeEach
    void setup() {
        // Clear the cache before each test to prevent stale data.
        final Cache usersCache = cacheManager.getCache("users");
        if (usersCache != null) {
            usersCache.clear(); // Ensure cache is empty before each test
        }

        // Initialize a test user entity for consistent testing
        userEntity = new UserEntity(null, "Ranzy", "Blessings", "blessingsihembi@gmail.com",
                "secret", null);
    }

    @Test
    void testFindByEmail_shouldCacheResult() {
        // Given: Mock the repository to return a test user when queried by email.
        Mockito.when(userRepository.findByEmail(Mockito.anyString())).thenReturn(Mono.just(userEntity));

        // When: Call the service method twice, expecting the result to be cached after the first call.
        UserDTO firstCallResult = userService.findByEmail(userEntity.email()).block();
        UserDTO secondCallResult = userService.findByEmail(userEntity.email()).block();

        // Then: Verify both calls return the same cached result and are not querying the repository again.
        assertNotNull(firstCallResult);
        assertNotNull(secondCallResult);
        assertEquals(firstCallResult, secondCallResult); // Ensure the results are identical (cached)

        // Verify the repository was called only once, confirming caching behavior.
        Mockito.verify(userRepository, Mockito.times(1)).findByEmail(Mockito.anyString());
    }
}
