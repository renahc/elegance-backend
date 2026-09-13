package com.elegance.appointment;

import com.elegance.appointment.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
// Excluimos la autoconfiguración de la base de datos para que NO intente conectarse durante las pruebas
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
class EleganceAppointmentServiceApplicationTests {

    // Mockeamos el repositorio para que Spring no intente crear la conexión real a la BD
    @MockBean
    private AppointmentRepository appointmentRepository;

    @Test
    void contextLoads() {
        // Si el test llega hasta aquí, significa que el contexto de Spring cargó exitosamente
        // sin intentar conectarse a ninguna base de datos real.
    }
}