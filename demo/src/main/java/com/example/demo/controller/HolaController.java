package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
@RestController
@RequestMapping("/api")

public class HolaController {
   @GetMapping("/conectar")
    public ApiResponse conectar() {
        // Aquí es donde el Back-end genera su respuesta
        String mensajeParaElFront = "Respuesta del back_end";
        
        if (true) {
            return new ApiResponse(mensajeParaElFront, true, null);
        }
        
        return new ApiResponse("Error", false, null);
    }
}
