package com.github.rblessings;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"dev"})
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UrlradarApplicationTest {

    @Test
    void contextLoads() {
        // Verifies that the Spring application context loads without errors.
    }
}
