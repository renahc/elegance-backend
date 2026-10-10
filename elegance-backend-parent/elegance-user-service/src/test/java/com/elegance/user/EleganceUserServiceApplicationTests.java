package com.elegance.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EleganceUserServiceApplicationTests {

    @Test
    void contextLoads() {
        // Este test solo verifica que el contexto de Spring carga correctamente
        // usando H2 en memoria, sin tocar MySQL real
    }
}