package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ApiResponse obtenerProyectos(@RequestParam(required = false) Boolean activo, @RequestParam(required = false) String fecha) {
        List<ProyectoDTO> lista = proyectoService.obtenerTodosLosProyectos();
        return new ApiResponse("Listado de proyectos recuperado", true, lista);
    }
    
    @PostMapping("/{id}")
    public ApiResponse actualizarProyecto(@PathVariable Long id, @RequestBody ProyectoDTO proyectoDTO) {
        // Recibe el ID del proyecto y los datos nuevos desde el cuerpo de la petición
        ProyectoDTO actualizado = proyectoService.actualizarProyecto(id, proyectoDTO);
        // Devuelve una respuesta con el proyecto ya actualizado
        return new ApiResponse("Proyecto actualizado correctamente", true, actualizado);
    }

    @GetMapping("/externos")
    public ApiResponse obtenerProyectosExternos() {
        // Llamamos al nuevo método que creamos en el servicio
        List<ProyectoDTO> listaExternos = proyectoService.obtenerProyectosGitLabNoRegistrados();
        
        return new ApiResponse("Proyectos de GitLab pendientes de importar", true, listaExternos);
    }
}