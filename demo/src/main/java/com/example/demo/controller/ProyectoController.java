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

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.services.ProyectoService;




@RestController
@RequestMapping("/api/proyectos")
@CrossOrigin(origins = "*")
public class ProyectoController {

    @Autowired
    private ProyectoService proyectoService;
    
   @GetMapping("/cargar")
public ApiResponse obtenerProyectos(
        @RequestParam(required = false) Boolean activo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

    List<ProyectoDTO> lista = proyectoService.obtenerTodosLosProyectos(activo, desde, hasta);
    return new ApiResponse("Listado de proyectos recuperado", true, lista);
}

    /*@GetMapping("/cargar")
    public ApiResponse obtenerProyectos(
    @RequestParam(required = false) Boolean activo,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {


        List<ProyectoDTO> lista = proyectoService.obtenerTodosLosProyectos(activo, desde, hasta);
        return new ApiResponse("Listado de proyectos recuperado", true, lista);
    }*/

    @PostMapping
    // @RequestBody: Recibe el JSON del formulario y lo convierte en el ProyectoDTO
    public ApiResponse crearProyecto(@RequestBody ProyectoDTO proyectoDTO) {
        try {
            //Mandamos el DTO al servicio para que lo guarde
            ProyectoDTO proyectoGuardado = proyectoService.crearProyecto(proyectoDTO);
            
            // Respondemos con éxito y devolvemos el proyecto ya con su ID generado
            return new ApiResponse("Proyecto creado correctamente", true, proyectoGuardado);
        } catch (Exception e) {
            // Si falta algún dato obligatorio o la base de datos falla
            return new ApiResponse("Error al crear el proyecto: " + e.getMessage(), false, null);
        }
    }

    @DeleteMapping("/{id}")
    public ApiResponse eliminarProyecto(@PathVariable Long id) {
        try {
            // Llamamos al servicio para ejecutar el borrado
            proyectoService.eliminarProyecto(id);
            
            // Respondemos con éxito
            return new ApiResponse("Proyecto eliminado permanentemente", true, null);
        } catch (Exception e) {
            // Si el ID no existe o hay un error de conexión
            return new ApiResponse("Error al eliminar: " + e.getMessage(), false, null);
        }
    }
    
    @PutMapping("/{id}")
    public ApiResponse actualizarProyecto(@PathVariable Long id, @RequestBody ProyectoDTO proyectoDTO) {
        // Recibe el ID del proyecto y los datos nuevos desde el cuerpo de la petición
        ProyectoDTO actualizado = proyectoService.actualizarProyecto(id, proyectoDTO);
        // Devuelve una respuesta con el proyecto ya actualizado
        return new ApiResponse("Proyecto actualizado correctamente", true, actualizado);
    }

}