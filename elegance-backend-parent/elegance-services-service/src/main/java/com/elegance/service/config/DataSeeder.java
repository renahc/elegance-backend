package com.elegance.service.config;

import com.elegance.service.model.ServiceEntity;
import com.elegance.service.repository.ServiceRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Arrays;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ServiceRepository serviceRepository;

    public DataSeeder(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @Override
    public void run(String... args) {
        if (serviceRepository.count() == 0) {
            serviceRepository.saveAll(Arrays.asList(
                new ServiceEntity("srv-1", "Balayage & Iluminación Premium", "Técnica avanzada de iluminación y degradado de color.", "cabello", 180, new BigDecimal("95000"), true, "https://images.unsplash.com/photo-1560066984-138dadb4c035?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-2", "Corte Estilizado + Brushing", "Corte personalizado con lavado y secado profesional.", "cabello", 60, new BigDecimal("32000"), true, "https://images.unsplash.com/photo-1562322140-8baeececf3df?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-3", "Tratamiento Reparador Olaplex", "Tratamiento intensivo para la reconstrucción de la fibra capilar.", "cabello", 75, new BigDecimal("45000"), true, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-4", "Manicura Rusa + Esmaltado Permanente", "Limpieza profunda de cutículas con torno y esmaltado de alta duración.", "uñas", 90, new BigDecimal("28000"), true, "https://images.unsplash.com/photo-1604654894610-df63bc536371?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-5", "Pedicura Spa Hidratante", "Tratamiento de exfoliación, masaje e hidratación profunda para pies.", "uñas", 60, new BigDecimal("24000"), true, "https://images.unsplash.com/photo-1519014816548-bf5fe059798b?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-6", "Facial Hidra-Lifting con Ácido Hialurónico", "Limpieza facial profunda con hidratación intensa y efecto reafirmante.", "facial", 75, new BigDecimal("58000"), true, "https://images.unsplash.com/photo-1570172619644-dfd03ed5d881?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-7", "Diseño de Cejas & Laminado HD", "Perfilado, diseño personalizado y laminado de cejas.", "facial", 45, new BigDecimal("22000"), true, "https://images.unsplash.com/photo-1522337360788-8b13dee7a37e?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-8", "Maquillaje Social & Novias", "Maquillaje profesional de alta duración para eventos y novias.", "maquillaje", 90, new BigDecimal("65000"), true, "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&q=80&w=800"),
                new ServiceEntity("srv-9", "Masaje Relajante Piedras Volcánicas", "Masaje corporal descontracturante con piedras térmicas.", "spa", 60, new BigDecimal("42000"), true, "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&q=80&w=800")
            ));
        }
    }
}
