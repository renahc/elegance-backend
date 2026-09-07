package com.elegance.salon.config;

import com.elegance.salon.model.*;
import com.elegance.salon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ServiceRepository serviceRepository;
    private final StylistRepository stylistRepository;
    private final ClientRepository clientRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;

    @Autowired
    public DataSeeder(
            ServiceRepository serviceRepository,
            StylistRepository stylistRepository,
            ClientRepository clientRepository,
            AppointmentRepository appointmentRepository,
            NotificationRepository notificationRepository
    ) {
        this.serviceRepository = serviceRepository;
        this.stylistRepository = stylistRepository;
        this.clientRepository = clientRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (serviceRepository.count() == 0) {
            serviceRepository.saveAll(List.of(
                new ServiceEntity("srv-1", "Balayage & Iluminación Premium", "cabello", 95000, 180, true, true),
                new ServiceEntity("srv-2", "Corte Estilizado + Brushing", "cabello", 32000, 60, true, true),
                new ServiceEntity("srv-3", "Tratamiento Reparador Olaplex", "cabello", 45000, 75, false, true),
                new ServiceEntity("srv-4", "Manicura Rusa + Esmaltado Permanente", "uñas", 28000, 90, true, true),
                new ServiceEntity("srv-5", "Pedicura Spa Hidratante", "uñas", 24000, 60, false, true),
                new ServiceEntity("srv-6", "Facial Hidra-Lifting con Ácido Hialurónico", "facial", 58000, 75, true, true),
                new ServiceEntity("srv-7", "Diseño de Cejas & Laminado HD", "facial", 22000, 45, false, true),
                new ServiceEntity("srv-8", "Maquillaje Social & Novias", "maquillaje", 65000, 90, true, true),
                new ServiceEntity("srv-9", "Masaje Relajante Piedras Volcánicas", "spa", 42000, 60, false, true)
            ));
        }

        if (stylistRepository.count() == 0) {
            stylistRepository.saveAll(List.of(
                new StylistEntity("sty-1", "Valentina Morales", "Master Hair Specialist", "Colorimetría & Balayage",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                        4.9, 142, true, "09:00 - 18:00", 4),
                new StylistEntity("sty-2", "Camila Rosselot", "Senior Nail Artist", "Manicura Rusa & Soft Gel",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300",
                        4.8, 98, true, "10:00 - 19:00", 5),
                new StylistEntity("sty-3", "Isadora Benítez", "Cosmiatra & Skin Expert", "Faciales Avanzados & Peeling",
                        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=300",
                        5.0, 115, false, "11:00 - 20:00", 3),
                new StylistEntity("sty-4", "Mateo Fuentes", "Senior Hair Stylist", "Cortes Tendencia & Peinados",
                        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300",
                        4.7, 76, true, "09:30 - 18:30", 2)
            ));
        }

        if (clientRepository.count() == 0) {
            clientRepository.saveAll(List.of(
                new ClientEntity("cli-1", "Sofia Larraín", "sofia.larrain@gmail.com", "+56 9 8765 4321",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=300",
                        14, 480000, "2026-08-28", "VIP", "Prefiere café americano con leches vegetales."),
                new ClientEntity("cli-2", "Fernanda Valenzuela", "fer.valenzuela@outlook.com", "+56 9 7654 3210",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300",
                        8, 260000, "2026-08-20", "Frecuente", "Cliente puntual."),
                new ClientEntity("cli-3", "Mariana Silva", "marianasilva.design@gmail.com", "+56 9 6543 2109",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                        3, 125000, "2026-08-15", "Regular", null),
                new ClientEntity("cli-4", "Constanza Silva", "coni.silva@empresa.cl", "+56 9 5432 1098",
                        "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300",
                        1, 58000, "2026-08-30", "Nuevo", "Primera visita aprobada."),
                new ClientEntity("cli-5", "Antonia Edwards", "antonia.edwards@gmail.com", "+56 9 4321 0987",
                        "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=80&w=300",
                        22, 890000, "2026-08-25", "VIP", "Asiste cada 3 semanas.")
            ));
        }

        if (appointmentRepository.count() == 0) {
            appointmentRepository.saveAll(List.of(
                new AppointmentEntity("apt-101", "cli-1", "Sofia Larraín",
                        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=300",
                        "srv-1", "Balayage & Iluminación Premium", "cabello", "sty-1", "Valentina Morales",
                        "2026-08-31", "10:00", 180, 95000, "en_proceso", "Tono rubio ceniza cálido."),
                new AppointmentEntity("apt-102", "cli-2", "Fernanda Valenzuela",
                        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300",
                        "srv-4", "Manicura Rusa + Esmaltado", "uñas", "sty-2", "Camila Rosselot",
                        "2026-08-31", "11:30", 90, 28000, "confirmada", "Tono Rose Gold brillante."),
                new AppointmentEntity("apt-103", "cli-3", "Mariana Silva",
                        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                        "srv-6", "Facial Hidra-Lifting", "facial", "sty-3", "Isadora Benítez",
                        "2026-08-31", "14:00", 75, 58000, "confirmada", null)
            ));
        }

        if (notificationRepository.count() == 0) {
            notificationRepository.saveAll(List.of(
                new NotificationEntity("notif-1", "Nueva Cita Confirmada", "Sofia Larraín reservó Balayage Premium.", "Hace 10 min", false, "appointment"),
                new NotificationEntity("notif-2", "Cliente VIP Agendado", "Antonia Edwards reservó Maquillaje Social.", "Hace 35 min", false, "client")
            ));
        }
    }
}
