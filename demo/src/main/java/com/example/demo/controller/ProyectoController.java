package com.example.demo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.services.ProyectoService;

@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class ProyectoController {

    @Autowired
    private ProyectoService proyectoService;

    /**
     * Metodo que recibe parametros a medio de filtros para devolver los proyectos de la base de datos en base a los filtros(si estos son nulos se devolveran todos los preoyectos)
     * @param activo determina si los proyectos deben estar activos o no
     * @param desde determina la fecha de inicio desde la cual se muestran los proyectos
     * @param hasta determina la fecha de fin hasta la cual se muestran los proyectos
     * @return ApiResponse json con los proyectos que siguen los filtros
     */
    @GetMapping("/cargar")
    public ResponseEntity<ApiResponse> obtenerProyectos(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            List<ProyectoDTO> lista = proyectoService.obtenerTodosLosProyectos(activo, desde, hasta);
            return ResponseEntity.ok(new ApiResponse("Listado de proyectos recuperado", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al cargar los proyectos: " + e.getMessage(), false, null));
        }
    }

    /**
     * Metodo que recibe un proyecto y lo guarda en la base de datos
     * @param proyectoDTO objeto a guardar en la base de datos
     * @return ApiResponse json que contiene el proyecto guardado si el guardado ha tenido exito o nada si el guardado no se ha realizado (sea porqeu el proyecto ya existe o porque no ha pasado los filtros)
     */
    @PostMapping
    // @RequestBody: Recibe el JSON del formulario y lo convierte en el ProyectoDTO
    @PreAuthorize("hasAnyAuthority('SuperAdministrador', 'Administrador')")
    public ResponseEntity<ApiResponse> crearProyecto(@RequestBody ProyectoDTO proyectoDTO) {
        try {
            ProyectoDTO proyectoGuardado = proyectoService.crearProyecto(proyectoDTO);
            return ResponseEntity.ok(new ApiResponse("Proyecto creado correctamente", true, proyectoGuardado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al crear el proyecto: " + e.getMessage(), false, null));
        }
    }

    /**
     * Metodo que recibe un id de un proyecto y lo elimina de la base de datos
     * @param id id del proyecto a eliminar
     * @return ApiResponse json que contiene true si se ha eliminado o false si no
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SuperAdministrador')")
    public ResponseEntity<ApiResponse> eliminarProyecto(@PathVariable Long id) {
        try {
            ProyectoDTO proyectoEliminado = proyectoService.eliminarProyecto(id);
            return ResponseEntity.ok(new ApiResponse("Proyecto eliminado permanentemente", true, proyectoEliminado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al eliminar: " + e.getMessage(), false, null));
        }
    }

    /**
     * Metodo que recibe un id de proyecto y un objeto proyectoDTO para actualizar la informacion del proyecto con la id en la base de datos
     * @param id id del proyecto a actualizar
     * @param proyectoDTO objeto proyectoDTO con la nueva informacion
     * @return ApiResponse json que contiene el proyecto actualizado
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SuperAdministrador', 'Administrador')")
    public ResponseEntity<ApiResponse> actualizarProyecto(@PathVariable Long id, @RequestBody ProyectoDTO proyectoDTO) {
        try {
            ProyectoDTO actualizado = proyectoService.actualizarProyecto(id, proyectoDTO);
            return ResponseEntity.ok(new ApiResponse("Proyecto actualizado correctamente", true, actualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al actualizar el proyecto: " + e.getMessage(), false, null));
        }
    }

    /**
     * Alias de compatibilidad para clientes que aun envian POST al editar.
     */
    @PostMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SuperAdministrador', 'Administrador')")
    public ResponseEntity<ApiResponse> actualizarProyectoCompat(@PathVariable Long id, @RequestBody ProyectoDTO proyectoDTO) {
        try {
            ProyectoDTO actualizado = proyectoService.actualizarProyecto(id, proyectoDTO);
            return ResponseEntity.ok(new ApiResponse("Proyecto actualizado correctamente (Compatibilidad)", true, actualizado));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al actualizar el proyecto: " + e.getMessage(), false, null));
        }
    }

}
