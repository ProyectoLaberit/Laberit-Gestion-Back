package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.DepartamentoDTO;
import com.example.demo.services.DepartamentoService;

@RestController
@RequestMapping("/api/departamentos")
@CrossOrigin(origins = "*")
public class DepartamentoController {

    @Autowired
    private DepartamentoService departamentoService; // <--- Usamos el Service, no el Repository

    @GetMapping
    public ApiResponse listarDepartamentos() {
        try {
            // El service ya nos devuelve DTOs, evitando el bucle infinito
            List<DepartamentoDTO> departamentos = departamentoService.listarTodos();
            return new ApiResponse("Departamentos recuperados", true, departamentos);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar: " + e.getMessage(), false, null);
        }
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_SUPERADMINISTRADOR')") // <--- Seguridad
    public ApiResponse crearDepartamento(@RequestBody DepartamentoDTO dto) {
        try {
            departamentoService.crear(dto);
            return new ApiResponse("Departamento creado con éxito", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al crear: " + e.getMessage(), false, null);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMINISTRADOR')")
    public ApiResponse actualizarDepartamento(@PathVariable int id, @RequestBody DepartamentoDTO dto) {
        try {
            departamentoService.actualizar(id, dto);
            return new ApiResponse("Departamento actualizado", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al actualizar: " + e.getMessage(), false, null);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMINISTRADOR')")
    public ApiResponse eliminarDepartamento(@PathVariable int id) {
        try {
            departamentoService.eliminar(id);
            return new ApiResponse("Departamento eliminado", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al eliminar: Es posible que tenga sub-departamentos dependientes", false,
                    null);
        }
    }
}