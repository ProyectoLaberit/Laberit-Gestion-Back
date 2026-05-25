package com.example.demo.dto.excel;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilaAvanceSemanalDTO {
    private String semana; // Ej: "Semana 1"
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private double horasEstimadas;
    private double horasReales;
    private double desviacionHoras;
    private double desviacionPorcentaje;
}
