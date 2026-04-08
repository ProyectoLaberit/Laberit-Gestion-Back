package com.example.demo.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.services.ProyectoService;
import com.example.demo.services.UsuarioService;


@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ProyectoService proyectoService; // Paso 3: Inyección necesaria

    @PostMapping("/login")
    public ApiResponse verificar(@RequestBody LoginRequest login) {
        // Validamos las credenciales
        boolean esValido = usuarioService.validarUsuario(login);
        
        if (esValido) {
            // Paso 4: Recuperamos la lista de proyectos
            List<ProyectoDTO> proyectos = proyectoService.obtenerTodosLosProyectos(null, null, null); // Puedes ajustar los filtros según tus necesidades
            // La enviamos dentro del campo 'data' del ApiResponse
            return new ApiResponse("Login exitoso", true, proyectos);
        } else {
            return new ApiResponse("Credenciales inválidas", false, null);
        }
    }
}