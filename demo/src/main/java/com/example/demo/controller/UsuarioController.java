package com.example.demo.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.UsuarioDTO;
import com.example.demo.services.UsuarioService;
import com.example.demo.entity.Usuario;
import com.example.demo.security.JwtUtil;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "*")

public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse verificar(@RequestBody LoginRequest login) {

        Usuario usuario = usuarioService.validarUsuario(login);

        if (usuario != null) {
            // Como no es null, sabemos que el login fue un éxito. Generamos su Token.
            String token = jwtUtil.generarToken(usuario);

            // Empaquetamos la respuesta para el Front-end
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("id", usuario.getId());
            data.put("nombre", usuario.getNombre());
            data.put("email", usuario.getEmail());
            data.put("foto", usuario.getFoto());
            if (!usuario.getRoles().isEmpty()) {
                data.put("rol", usuario.getRoles().get(0).getNombre());
            }

            return new ApiResponse("Login exitoso", true, data);
        } else {
            return new ApiResponse("Credenciales inválidas", false, null);
        }
    }

    // ... (El resto de tus endpoints se mantienen exactamente igual: crearUsuario,
    // eliminarUsuario, etc.)

    @PostMapping
    public ApiResponse crearUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        try {
            // Llamamos al servicio que hashea la contraseña y comprueba el email
            UsuarioDTO usuarioCreado = usuarioService.crearUsuario(usuarioDTO);

            // Si todo va bien, devolvemos el usuario creado
            return new ApiResponse("Usuario creado con éxito", true, usuarioCreado);

        } catch (RuntimeException e) {
            // Si salta el error de "Ya existe un usuario con este email"
            return new ApiResponse(e.getMessage(), false, null);

        } catch (Exception e) {
            // Por si hay algún fallo de conexión con la base de datos
            return new ApiResponse("Error inesperado al crear el usuario: " + e.getMessage(), false, null);
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse eliminarUsuario(@PathVariable Integer id) {
        try {
            // Llamamos al servicio para que ejecute el borrado
            usuarioService.eliminarUsuario(id);

            // Si llega hasta aquí, es que no ha habido errores
            return new ApiResponse("Usuario eliminado correctamente", true, null);

        } catch (RuntimeException e) {
            // Capturamos el error "El usuario no existe"
            return new ApiResponse(e.getMessage(), false, null);

        } catch (Exception e) {
            // Por si hay algún fallo general de base de datos
            return new ApiResponse("Error al intentar eliminar el usuario: " + e.getMessage(), false, null);
        }
    }

    @PutMapping("/{id}/password")
    public ApiResponse cambiarContrasena(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            // Verificamos que nos hayan enviado ambas contraseñas
            if (dto.getPasswordVieja() == null || dto.getPasswordNueva() == null) {
                return new ApiResponse("Faltan datos: Debes enviar la contraseña vieja y la nueva.", false, null);
            }

            // Llamamos al servicio
            usuarioService.cambiarContrasena(id, dto.getPasswordVieja(), dto.getPasswordNueva());

            return new ApiResponse("Contraseña actualizada correctamente.", true, null);

        } catch (RuntimeException e) {
            // Capturamos el error si la contraseña actual no coincide
            return new ApiResponse(e.getMessage(), false, null);

        } catch (Exception e) {
            return new ApiResponse("Error inesperado al cambiar la contraseña: " + e.getMessage(), false, null);
        }
    }

    @PutMapping(value = "/{id}/foto", consumes = "multipart/form-data")
    public ApiResponse cambiarFoto(
            @PathVariable Integer id,
            @RequestParam(value = "archivo", required = false) MultipartFile archivo) {

        try {
            // Llamamos al servicio (él ya se encarga de ignorarlo si el archivo viene nulo
            // o vacío)
            usuarioService.cambiarFoto(id, archivo);

            return new ApiResponse("Proceso de actualización de foto completado.", true, null);

        } catch (RuntimeException e) {
            // Capturamos si el usuario no existe o hay algún error de validación
            return new ApiResponse(e.getMessage(), false, null);

        } catch (Exception e) {
            return new ApiResponse("Error inesperado al guardar la foto: " + e.getMessage(), false, null);
        }
    }

    @PutMapping("/{id}/rol")
    public ApiResponse cambiarRol(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            // Verificamos que nos hayan mandado algo en el campo rol
            if (dto.getRol() == null || dto.getRol().trim().isEmpty()) {
                return new ApiResponse("No se solicitó cambio de rol.", true, null);
            }

            // Llamamos al servicio
            usuarioService.cambiarRol(id, dto.getRol());

            return new ApiResponse("Rol actualizado correctamente.", true, null);

        } catch (RuntimeException e) {
            // Capturamos si el usuario o el rol no existen
            return new ApiResponse(e.getMessage(), false, null);

        } catch (Exception e) {
            return new ApiResponse("Error inesperado al cambiar el rol: " + e.getMessage(), false, null);
        }
    }

    @PutMapping("/{id}")
    public ApiResponse actualizarUsuario(@PathVariable Integer id, @RequestBody UsuarioDTO dto) {
        try {
            UsuarioDTO actualizado = usuarioService.actualizarUsuario(id, dto);
            return new ApiResponse("Usuario actualizado correctamente", true, actualizado);
        } catch (Exception e) {
            return new ApiResponse(e.getMessage(), false, null);
        }
    }
}