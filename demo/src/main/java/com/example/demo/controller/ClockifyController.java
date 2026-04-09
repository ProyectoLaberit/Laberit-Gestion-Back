package com.example.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ProyectoDTO;
import com.example.demo.services.ProyectoService;

@RestController
@RequestMapping("/api/clockify") // Ruta base diferente para no chocar con ProyectoController
public class ClockifyController {

    @Autowired
    private ProyectoService proyectoService;

    /**
     * Endpoint para obtener proyectos de Clockify que aún no están en nuestra base de datos.
     * URL: GET http://localhost:8080/api/clockify/pendientes
     */
    @GetMapping("/pendientes")
    public List<ProyectoDTO> obtenerPendientes() {
        return proyectoService.obtenerProyectosClockifyNoRegistrados();
    }
}