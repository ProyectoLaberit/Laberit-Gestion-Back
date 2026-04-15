package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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
     * Endpoint para cargar los desplegables de Fases y Subfases en el Frontend.
     */
    @GetMapping("/jerarquia")
    public ApiResponse obtenerJerarquiaFases() {
        try {
            List<FaseDTO> jerarquia = faseService.obtenerJerarquiaFases();
            return new ApiResponse("Jerarquía de fases recuperada", true, jerarquia);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar jerarquía: " + e.getMessage(), false, null);
        }
    }
}