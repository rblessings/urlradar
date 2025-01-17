package com.github.rblessings;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Test to verify that the Spring application context loads successfully
 * with the 'dev' profile. This ensures that all necessary beans and
 * configurations for the 'dev' environment are initialized correctly.
 */
@ActiveProfiles({"dev"})
@SpringBootTest
class UrlradarApplicationTest {

    @Test
    void contextLoads() {
        // Test the context loading, no additional code required.
    }
}
