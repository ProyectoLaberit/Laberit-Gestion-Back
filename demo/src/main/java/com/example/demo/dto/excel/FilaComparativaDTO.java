package com.example.demo.dto.excel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilaComparativaDTO {
    private String idGitlab;
    private String fase;
    private String tarea;
    private String departamento;
    private double estimacionMinima;
    private double estimacionMaxima;
    private double horasReales;
    private double desviacionHoras;
    private double desviacionPorcentaje;
    private String estadoGitlab; // "Open" o "Closed"
}
