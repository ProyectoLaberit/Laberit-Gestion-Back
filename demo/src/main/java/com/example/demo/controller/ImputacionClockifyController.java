package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.services.ImputacionClockifyService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/imputaciones")
public class ImputacionClockifyController {

    @Autowired
    private ImputacionClockifyService service;

    /**
     * Metodo que devuelve las imputaciones de una tarea de cierta subfase de cierto proyecto
     * @param idProyecto id del proyecto
     * @param idTareaProyecto id de la tarea a buscar
     * @param idDepartamento id del departamento a buscar 
     * @param subfase nombre de la subfase donde buscar, opcional
     * @return ApiResponse json que contiene la lista de las imputaciones del departamento en la tarea
     */
    @GetMapping("/departamento/{idProyecto}/{idTareaProyecto}/{idDepartamento}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ResponseEntity<ApiResponse> obtenerPorDepartamento(
            @PathVariable Long idProyecto, 
            @PathVariable Long idTareaProyecto, 
            @PathVariable Integer idDepartamento,
            @RequestParam(required = false, defaultValue = "") String subfase) {
        try {
            List<ImputacionClockify> lista = service.obtenerPorDepartamentoYTarea(idProyecto, idTareaProyecto, idDepartamento, subfase);
            return ResponseEntity.ok(new ApiResponse("Imputaciones obtenidas", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al obtener imputaciones", false, null));
        }
    }
    /**
     * Metodo para obtener las imputaciones de clockify de un departamento correctamente escritas
     * @param idProyecto id del proyecto donde buscar
     * @param idTareaProyecto id de la tarea que comprobar
     * @param idDepartamento id del departamento al que pertenecen las imputaciones
     * @return ApiResponse json que contiene el numero de imputaciones escritas correctamente
     */
    @GetMapping("/departamento/{idProyecto}/{idTareaProyecto}/{idDepartamento}/validas/count")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ResponseEntity<ApiResponse> contarValidasDepartamento(
            @PathVariable Long idProyecto, 
            @PathVariable Long idTareaProyecto, 
            @PathVariable Integer idDepartamento) {
        try {
            Integer total = service.contarValidasPorDepartamento(idProyecto, idTareaProyecto, idDepartamento);
            return ResponseEntity.ok(new ApiResponse("Recuento de válidas", true, total));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al contar válidas", false, 0));
        }
    }

    /**
     * Metodo para obtener las imputaciones de clockify de un departamento mal escritas
     * @param idProyecto id del proyecto donde buscar
     * @param idTareaProyecto id de la tarea que comprobar
     * @param idDepartamento id del departamento al que pertenecen las imputaciones
     * @return ApiResponse json que contiene el numero de imputaciones escritas incorrectamente
     */
    @GetMapping("/departamento/{idProyecto}/{idTareaProyecto}/{idDepartamento}/invalidas/count")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ResponseEntity<ApiResponse> contarInvalidasDepartamento(
            @PathVariable Long idProyecto, 
            @PathVariable Long idTareaProyecto, 
            @PathVariable Integer idDepartamento) {
        try {
            Integer total = service.contarInvalidasPorDepartamento(idProyecto, idTareaProyecto, idDepartamento);
            return ResponseEntity.ok(new ApiResponse("Recuento de inválidas", true, total));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al contar inválidas", false, 0));
        }
    }
    /**
     * Metodo que convierte una imputacion incorrecta en una correcta y viceversa
     * @param idImputacion id de la imputacion
     * @param idTareaProyecto id de la tarea a la que pertenece
     * @return ApiResponse con un boolean a true si el cambio se realiza correctamente y a false si no
     */
    @PutMapping("/alternar-validacion/{idImputacion}/{idTareaProyecto}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ResponseEntity<ApiResponse> alternarValidacion(
            @PathVariable Long idImputacion, 
            @PathVariable Long idTareaProyecto) {
        try {
            ImputacionClockify actualizada = service.alternarEstadoValidacion(idImputacion, idTareaProyecto);
            if (actualizada != null) {
                return ResponseEntity.ok(new ApiResponse("Estado e ID actualizados correctamente", true, actualizada));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("No se encontró la imputación", false, null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al procesar el cambio", false, null));
        }
    }
    /**
     * Metodo para editar la tarea_extraida, la subfase_extraida y reasignar la tarea estructuralmente
     * @param idImputacion id de la imputacion a cambiar
     * @param body json que contiene nueva tarea, subfase (texto) y el idFase (número)
     * @return ApiResponse con un boolean a true si el cambio se realiza correctamente y false si no
     */
    @PutMapping("/editar-tarea/{idImputacion}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ResponseEntity<ApiResponse> editarTarea(
            @PathVariable Long idImputacion, 
            @RequestBody java.util.Map<String, String> body) {
        try {
            // 1. Recogemos los datos del JSON
            String nuevaTarea = body.get("tareaExtraida");
            String nuevaSubfase = body.get("subfaseExtraida");
            String idFaseStr = body.get("idFase");

            // 2. Convertimos el ID de la fase a Integer (si nos lo han enviado)
            Integer idFase = null;
            if (idFaseStr != null && !idFaseStr.trim().isEmpty()) {
                idFase = Integer.parseInt(idFaseStr);
            }

            // 3. Se lo pasamos todo al Service
            ImputacionClockify actualizada = service.editarTareaExtraida(idImputacion, nuevaTarea, nuevaSubfase, idFase);
            
            if (actualizada != null) {
                return ResponseEntity.ok(new ApiResponse("Tarea editada correctamente", true, actualizada));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("No se encontró la imputación", false, null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al editar la tarea", false, null));
        }
    }
    /**
     * Meotod para borrar una imputacion de la base de datos
     * @param idImputacion id de la imputacion a borrar
     * @return ApiResponse con un boolean a true si el cambio se elimina correctamente y false si no
     */
    @DeleteMapping("/borrar/{idImputacion}")
    @PreAuthorize("hasAuthority('ROLE_SUPERADMINISTRADOR')")
    public ResponseEntity<ApiResponse> borrarImputacion(@PathVariable Long idImputacion) {
        try {
            boolean borrado = service.borrarImputacion(idImputacion);
            if (borrado) {
                return ResponseEntity.ok(new ApiResponse("Imputación borrada correctamente", true, null));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("No se encontró la imputación para borrar", false, null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al borrar la imputación", false, null));
        }
    }
    /**
     * metodo para filtrar las imputaciones obtenida por rango de fechas
     * @param idProyecto id del proyecto al que pertenecen
     * @param idTareaProyecto id de la tarea a la que pertenecen
     * @param idDepartamento id del departamento al que pertenecen
     * @param subfase nombre de la subfase a la que pertenecen
     * @param desde fecha desde la cual se quiere filtrar, opcional
     * @param hasta fecha hasta la cual se quiere buscar, opcional
     * @return ApiResponse json que contiene la lista de imputaciones obtenidas
     */
    @GetMapping("/departamento/{idProyecto}/{idTareaProyecto}/{idDepartamento}/fechas")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR', 'ROLE_EMPLEADO')")
    public ResponseEntity<ApiResponse> obtenerPorFechas(
            @PathVariable Long idProyecto, 
            @PathVariable Long idTareaProyecto, 
            @PathVariable Integer idDepartamento,
            @RequestParam(required = false, defaultValue = "") String subfase,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate desde,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate hasta) {
        try {
            if (desde.isAfter(hasta)) {
                return ResponseEntity.badRequest().body(new ApiResponse("La fecha 'desde' no puede ser posterior a 'hasta'", false, null));
            }
            List<ImputacionClockify> lista = service.filtrarPorFechas(idProyecto, idTareaProyecto, idDepartamento, subfase, desde, hasta);
            return ResponseEntity.ok(new ApiResponse("Imputaciones filtradas por fecha", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al filtrar por fechas", false, null));
        }
    }
}