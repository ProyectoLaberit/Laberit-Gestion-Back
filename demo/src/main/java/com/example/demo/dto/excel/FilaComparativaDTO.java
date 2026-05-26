package com.example.demo.dto.excel;




public interface FilaComparativaDTO {
    String getIdGitlab();
    String getFase();
    String getTarea();
    String getDepartamento();
    Double getEstimacionMinima();
    Double getEstimacionMaxima();
    Double getHorasReales();
    Double getDesviacionHoras();
    Double getDesviacionPorcentaje();
    String getEstadoGitlab();
}