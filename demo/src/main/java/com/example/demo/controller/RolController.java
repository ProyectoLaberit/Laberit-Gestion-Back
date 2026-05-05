package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.RolDTO;
import com.example.demo.services.RolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RolController {

    @Autowired
    private RolService rolService;

    /**
     * Metodo que devuelve los roles existentes en la base de datos
     * @return ApiResponse json que contiene los roles existentes en la base de datos
     */
    @GetMapping
    public ApiResponse obtenerRoles() {
        List<RolDTO> listaRoles = rolService.obtenerTodosLosRoles();
        return new ApiResponse("Listado de roles recuperado", true, listaRoles);
    }
}

