package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.conexion;
@RestController
@RequestMapping("/api")

public class HolaController {
   @GetMapping("/conectar")
    public ApiResponse conectar() {
        // Aquí es donde el Back-end genera su respuesta
        String mensajeParaElFront = "Respuesta del back_end";
        String[][] datos = conexion.cargarProyectos();
        
        if (true) {
            return new ApiResponse(datos, mensajeParaElFront, true, null);
        }
        
        return new ApiResponse(null, "Error", false, null);
    }
}
