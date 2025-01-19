package com.github.rblessings;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies that the Spring application context loads correctly with the 'dev' profile.
 * Ensures that all required beans and configurations for the 'dev' environment are initialized.
 */
@ActiveProfiles({"dev"})
@SpringBootTest
class UrlradarApplicationTest {

    @Test
    void contextLoads() {
        // Verify context loading; no further implementation needed.
    }
}
