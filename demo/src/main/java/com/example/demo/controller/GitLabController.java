package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.services.GitLabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gitlab")
@CrossOrigin(origins = "*")
public class GitLabController {

    @Autowired
    private GitLabService gitLabService;

    /**
     * Obtiene la lista de tareas (issues) de un proyecto específico consultando a
     * GitLab
     * mediante el ID del proyecto guardado en nuestra base de datos.
     */
    @GetMapping("/tareas/{proyectoId}")
    public ResponseEntity<ApiResponse> getTareas(@PathVariable Long proyectoId) {
        try {
            List<Map<String, Object>> tareas = gitLabService.obtenerTareasPorProyecto(proyectoId);
            return ResponseEntity.ok(new ApiResponse("Tareas recuperadas", true, tareas));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    /**
     * Consulta los proyectos en la cuenta de GitLab del usuario (usando su token)
     * y devuelve solo aquellos que todavía no han sido registrados en nuestro
     * sistema.
     */
    @GetMapping("/externos/{token_gitlab}")
    public ResponseEntity<ApiResponse> getExternos(@PathVariable String token_gitlab) {
        try {
            List<Map<String, Object>> externos = gitLabService.filtrarProyectosExternos(token_gitlab);
            return ResponseEntity.ok(new ApiResponse("Proyectos externos filtrados", true, externos));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(e.getMessage(), false, null));
        }
    }
}