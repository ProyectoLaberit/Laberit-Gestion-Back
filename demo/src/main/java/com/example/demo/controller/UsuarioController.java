package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.services.UsuarioService;


@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/login")
    public ApiResponse verificar(@RequestBody LoginRequest login) {
        boolean esValido = usuarioService.validarUsuario(login);
        
        if (esValido) {
            return new ApiResponse("Login exitoso", true, null);
        } else {
            return new ApiResponse("Credenciales inválidas", false, null);
        }
    }
}