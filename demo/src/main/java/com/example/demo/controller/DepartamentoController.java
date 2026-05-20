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

/* Controlador para gestionar las operaciones CRUD de departamentos
    * Este controlador expone endpoints para listar, crear, actualizar y eliminar departamentos.
    * Utiliza un servicio para manejar la lógica de negocio y un DTO para evitar problemas de serialización con entidades que tienen relaciones recursivas (departamento -> padre -> departamento).
    * La seguridad se implementa con anotaciones @PreAuthorize para restringir ciertas operaciones solo a usuarios con el rol "SUPERADMINISTRADOR".
*/

@RestController
@RequestMapping("/api/departamentos")
@CrossOrigin(origins = "*")
public class DepartamentoController {

    @Autowired
    private DepartamentoService departamentoService; // <--- Usamos el Service, no el Repository

    /**
     * Metodo que devuelve los departamentos existentes en la base de datos
     * @return ApiResponse json con los departamentos existentes en la base de datos
     */
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
    /**
     * Metodo para crear un departamento
     * @param dto objeto departamentoDTO con los datos del departamento a crear
     * @return ApiResponse con un boolean true si el departamento de crea sin problemas y false si hubo algun problema a la hora de crearlo
     */
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
    // 3. ACTUALIZAR
    /**
     * Metodo para actualizar la informacion de un departamento
     * @param id id del departamento a actualizar
     * @param dto objeto DepartamentoDTO con la nueva informacion del departamento
     * @return ApiResponse con un booleano true si la actuaizacion ha tenido exito y false si hubo algun problema
     */
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
    /**
     * Metodo para eliminar un departamento
     * @param id id del departamento a eliminar
     * @return ApiResponse con booleano a true si el borrado ha tenido exito y false si hubo algun problema
     */
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