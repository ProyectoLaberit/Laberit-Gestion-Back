package com.example.demo.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ProyectoDTO;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/usuarios")

public class UsuarioController  {

   @PostMapping("/login")
    public ApiResponse verificar(@RequestBody LoginRequest login) {
        
        if ("admin".equals(login.getEmail()) && "1234".equals(login.getPassword())) {
            
            // 1. Creamos una lista de proyectos de prueba
            List<ProyectoDTO> listaProyectos = new ArrayList<>();
            // listaProyectos.add(new ProyectoDTO( 2, "Proyecto A", "Descripción del Proyecto A", "2023-01-01", true));
            // listaProyectos.add(new ProyectoDTO( 2, "Proyecto B", "Descripción del Proyecto B", "2023-02-01", true));
            // listaProyectos.add(new ProyectoDTO( 2, "Proyecto C", "Descripción del Proyecto C", "2023-03-01", true));

            // 2. Metemos la lista en el campo 'data' del ApiResponse
            return new ApiResponse("Login exitoso", true, listaProyectos);
            
        } else {
            return new ApiResponse("Credenciales inválidas", false, null);
        }
    }
}
