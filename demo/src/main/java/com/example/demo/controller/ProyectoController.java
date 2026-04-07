package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.services.ProyectoService;




@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class ProyectoController {

    @Autowired
    private ProyectoService proyectoService;
    
    @GetMapping
    public ApiResponse obtenerProyectos() {
        List<ProyectoDTO> lista = proyectoService.obtenerTodosLosProyectos();
        return new ApiResponse("Listado de proyectos recuperado", true, lista);
    }

    @GetMapping("/externos")
    public ApiResponse obtenerProyectosExternos() {
        // Llamamos al nuevo método que creamos en el servicio
        List<ProyectoDTO> listaExternos = proyectoService.obtenerProyectosGitLabNoRegistrados();
        
        return new ApiResponse("Proyectos de GitLab pendientes de importar", true, listaExternos);
    }
}