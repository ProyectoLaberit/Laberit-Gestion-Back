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
public class ErrorVinculacionGitlabDTO {
    private String issueId;
    private String tituloGitlab;
    private String tareaProyecto; // Ej: "No enlazada (Huérfana)"
    private String estadoGitlab;
    private boolean vinculacionValida; // true = Verde, false = Rojo
}
