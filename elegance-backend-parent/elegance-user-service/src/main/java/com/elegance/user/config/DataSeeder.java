package com.elegance.user.config;

import com.elegance.user.model.ClientEntity;
import com.elegance.user.model.StylistEntity;
import com.elegance.user.repository.ClientRepository;
import com.elegance.user.repository.StylistRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ClientRepository clientRepository;
    private final StylistRepository stylistRepository;

    public DataSeeder(ClientRepository clientRepository, StylistRepository stylistRepository) {
        this.clientRepository = clientRepository;
        this.stylistRepository = stylistRepository;
    }

    @Override
    public void run(String... args) {
        if (clientRepository.count() == 0) {
            clientRepository.saveAll(Arrays.asList(
                new ClientEntity(
                    "cli-1", "Sofia Larraín", "sofia.larrain@gmail.com", "+56 9 8765 4321",
                    "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&q=80&w=300",
                    14, 480000, "2026-08-28", "VIP", "Prefiere café americano con leches vegetales. Sensible al tinte amoniacal."
                ),
                new ClientEntity(
                    "cli-2", "Fernanda Valenzuela", "fer.valenzuela@outlook.com", "+56 9 7654 3210",
                    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&q=80&w=300",
                    8, 260000, "2026-08-20", "Frecuente", "Cliente puntual. Siempre solicita manicura permanente tono nude."
                ),
                new ClientEntity(
                    "cli-3", "Mariana Silva", "marianasilva.design@gmail.com", "+56 9 6543 2109",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                    3, 125000, "2026-08-15", "Regular", null
                ),
                new ClientEntity(
                    "cli-4", "Constanza Silva", "coni.silva@empresa.cl", "+56 9 5432 1098",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300",
                    1, 58000, "2026-08-30", "Nuevo", "Primera visita aprobada con excelente evaluación."
                ),
                new ClientEntity(
                    "cli-5", "Antonia Edwards", "antonia.edwards@gmail.com", "+56 9 4321 0987",
                    "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&q=80&w=300",
                    22, 890000, "2026-08-25", "VIP", "Asiste cada 3 semanas para retoque de Balayage y faciales."
                )
            ));
        }

        if (stylistRepository.count() == 0) {
            stylistRepository.saveAll(Arrays.asList(
                new StylistEntity(
                    "sty-1", "Valentina Morales", "Master Hair Specialist", "Colorimetría & Balayage",
                    "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=300",
                    4.9, 142, true, "09:00 - 18:00", 4
                ),
                new StylistEntity(
                    "sty-2", "Camila Rosselot", "Senior Nail Artist", "Manicura Rusa & Soft Gel",
                    "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&q=80&w=300",
                    4.8, 98, true, "10:00 - 19:00", 5
                ),
                new StylistEntity(
                    "sty-3", "Isadora Benítez", "Cosmiatra & Skin Expert", "Faciales Avanzados & Peeling",
                    "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&q=80&w=300",
                    5.0, 115, false, "11:00 - 20:00", 3
                ),
                new StylistEntity(
                    "sty-4", "Mateo Fuentes", "Senior Hair Stylist", "Cortes Tendencia & Peinados",
                    "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=300",
                    4.7, 76, true, "09:30 - 18:30", 2
                )
            ));
        }
    }
}
