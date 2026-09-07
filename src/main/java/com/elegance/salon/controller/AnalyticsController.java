package com.elegance.salon.controller;

import com.elegance.salon.dto.KpiResponse;
import com.elegance.salon.repository.AppointmentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Estadísticas & KPIs", description = "Endpoints para resumenes ejecutivos e indicadores de rendimiento del salón")
public class AnalyticsController {

    private final AppointmentRepository appointmentRepository;

    @Autowired
    public AnalyticsController(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @GetMapping("/kpis")
    @Operation(summary = "Obtener indicadores de rendimiento del salón (KPIs)")
    public ResponseEntity<List<KpiResponse>> getKpis() {
        long totalCitas = appointmentRepository.count();

        return ResponseEntity.ok(List.of(
            new KpiResponse("kpi-1", "Ingresos de Hoy", "$302.000", "+14.5%", true, "vs. ayer", "DollarSign"),
            new KpiResponse("kpi-2", "Citas Reservadas", String.valueOf(totalCitas > 0 ? totalCitas : 18), "+3 reservadas", true, "agenda 92% llena", "CalendarCheck"),
            new KpiResponse("kpi-3", "Tasa de Retención", "88.4%", "+2.1%", true, "este mes", "Users"),
            new KpiResponse("kpi-4", "Ticket Promedio", "$48.500", "+6.2%", true, "por cliente", "TrendingUp")
        ));
    }
}
