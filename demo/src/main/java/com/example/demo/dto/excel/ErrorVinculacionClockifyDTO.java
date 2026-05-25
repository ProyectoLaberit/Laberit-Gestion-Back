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
public class ErrorVinculacionClockifyDTO {
    private String usuario;
    private String descripcionOriginal;
    private LocalDate fecha;
    private double horasPerdidas;
    private boolean vinculacionValida;
}
