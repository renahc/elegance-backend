package com.elegance.appointment.config;

import com.elegance.appointment.model.AppointmentEntity;
import com.elegance.appointment.repository.AppointmentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AppointmentRepository appointmentRepository;

    public DataSeeder(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public void run(String... args) {
        if (appointmentRepository.count() == 0) {
            AppointmentEntity apt101 = new AppointmentEntity();
            apt101.setId("apt-101");
            apt101.setClientId("cli-1");
            apt101.setClientName("Sofia Larraín");
            apt101.setClientAvatar("https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=300");
            apt101.setServiceId("srv-1");
            apt101.setServiceName("Balayage & Iluminación Premium");
            apt101.setServiceCategory("cabello");
            apt101.setStylistId("sty-1");
            apt101.setStylistName("Valentina Morales");
            apt101.setDate("2026-08-31");
            apt101.setTime("10:00");
            apt101.setDurationMinutes(180);
            apt101.setPrice(95000);
            apt101.setStatus("en_proceso");
            apt101.setNotes("Tono rubio ceniza cálido.");

            AppointmentEntity apt102 = new AppointmentEntity();
            apt102.setId("apt-102");
            apt102.setClientId("cli-2");
            apt102.setClientName("Fernanda Valenzuela");
            apt102.setClientAvatar("https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300");
            apt102.setServiceId("srv-4");
            apt102.setServiceName("Manicura Rusa + Esmaltado");
            apt102.setServiceCategory("uñas");
            apt102.setStylistId("sty-2");
            apt102.setStylistName("Camila Rosselot");
            apt102.setDate("2026-08-31");
            apt102.setTime("11:30");
            apt102.setDurationMinutes(90);
            apt102.setPrice(28000);
            apt102.setStatus("confirmada");
            apt102.setNotes("Tono Rose Gold brillante.");

            AppointmentEntity apt103 = new AppointmentEntity();
            apt103.setId("apt-103");
            apt103.setClientId("cli-3");
            apt103.setClientName("Mariana Silva");
            apt103.setClientAvatar("https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300");
            apt103.setServiceId("srv-6");
            apt103.setServiceName("Facial Hidra-Lifting");
            apt103.setServiceCategory("facial");
            apt103.setStylistId("sty-3");
            apt103.setStylistName("Isadora Benítez");
            apt103.setDate("2026-08-31");
            apt103.setTime("14:00");
            apt103.setDurationMinutes(75);
            apt103.setPrice(58000);
            apt103.setStatus("confirmada");

            AppointmentEntity apt104 = new AppointmentEntity();
            apt104.setId("apt-104");
            apt104.setClientId("cli-4");
            apt104.setClientName("Constanza Silva");
            apt104.setClientAvatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300");
            apt104.setServiceId("srv-2");
            apt104.setServiceName("Corte Estilizado + Brushing");
            apt104.setServiceCategory("cabello");
            apt104.setStylistId("sty-4");
            apt104.setStylistName("Mateo Fuentes");
            apt104.setDate("2026-08-31");
            apt104.setTime("15:30");
            apt104.setDurationMinutes(60);
            apt104.setPrice(32000);
            apt104.setStatus("confirmada");

            AppointmentEntity apt105 = new AppointmentEntity();
            apt105.setId("apt-105");
            apt105.setClientId("cli-5");
            apt105.setClientName("Antonia Edwards");
            apt105.setClientAvatar("https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=80&w=300");
            apt105.setServiceId("srv-8");
            apt105.setServiceName("Maquillaje Social & Novias");
            apt105.setServiceCategory("maquillaje");
            apt105.setStylistId("sty-1");
            apt105.setStylistName("Valentina Morales");
            apt105.setDate("2026-08-31");
            apt105.setTime("17:00");
            apt105.setDurationMinutes(90);
            apt105.setPrice(65000);
            apt105.setStatus("confirmada");

            AppointmentEntity apt100 = new AppointmentEntity();
            apt100.setId("apt-100");
            apt100.setClientId("cli-4");
            apt100.setClientName("Constanza Silva");
            apt100.setClientAvatar("https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300");
            apt100.setServiceId("srv-5");
            apt100.setServiceName("Pedicura Spa Hidratante");
            apt100.setServiceCategory("uñas");
            apt100.setStylistId("sty-2");
            apt100.setStylistName("Camila Rosselot");
            apt100.setDate("2026-08-31");
            apt100.setTime("09:00");
            apt100.setDurationMinutes(60);
            apt100.setPrice(24000);
            apt100.setStatus("completada");

            appointmentRepository.saveAll(Arrays.asList(apt101, apt102, apt103, apt104, apt105, apt100));
        }
    }
}
