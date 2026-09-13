package com.elegance.user;

import com.elegance.user.repository.ClientRepository;
import com.elegance.user.repository.StylistRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
// Excluimos la autoconfiguración de la base de datos para que NO intente conectarse durante las pruebas
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class EleganceUserServiceApplicationTests {

    @MockBean
    private ClientRepository clientRepository;

    @MockBean
    private StylistRepository stylistRepository;

    @Test
    void contextLoads() {
        // Si el test llega hasta aquí, significa que el contexto de Spring cargó exitosamente
        // sin intentar conectarse a ninguna base de datos real.
    }
}