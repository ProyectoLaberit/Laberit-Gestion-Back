package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.FaseDTO;
import com.example.demo.services.FaseService;

@RestController
@RequestMapping("/api/fases")
@CrossOrigin(origins = "*")
public class FaseController {

    @Autowired
    private FaseService faseService;

   /**
     * Endpoint para cargar las Fases y Subfases que contienen tareas para un proyecto específico.
     * Recibe el ID del proyecto como parámetro y devuelve una jerarquía de fases con sus subfases activas.
     */
    @GetMapping("/{idProyecto}")
   public ApiResponse obtenerJerarquiaFases(@PathVariable Long idProyecto) {
        try {
            List<FaseDTO> jerarquia = faseService.obtenerJerarquiaFasesPorProyecto(idProyecto);
            
            if (jerarquia.isEmpty()) {
                return new ApiResponse("El proyecto no tiene tareas o excel activo", true, jerarquia);
            }
            
            return new ApiResponse("Jerarquía de fases activa recuperada", true, jerarquia);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar jerarquía: " + e.getMessage(), false, null);
        }
    }
}