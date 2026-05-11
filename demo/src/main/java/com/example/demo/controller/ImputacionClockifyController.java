package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.services.ImputacionClockifyService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/imputaciones")
public class ImputacionClockifyController {

   @Autowired
    private ImputacionClockifyService service;

    // GET: Para rellenar la tabla de imputaciones específicas que componen una tarea
    @GetMapping("/validas/{idDetalleEstimacion}")
    public ResponseEntity<ApiResponse> obtenerValidas(@PathVariable Long idDetalleEstimacion) {
        try {
            List<ImputacionClockify> lista = service.obtenerImputacionesValidas(idDetalleEstimacion);
            return ResponseEntity.ok(new ApiResponse("Lista de imputaciones obtenida", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al obtener imputaciones", false, null));
        }
    }

    // GET: Buscar las imputaciones huérfanas por idProyecto
    @GetMapping("/huerfanas/{idProyecto}")
    public ResponseEntity<ApiResponse> obtenerHuerfanas(@PathVariable Long idProyecto) {
        try {
            List<ImputacionClockify> lista = service.obtenerHuerfanas(idProyecto);
            return ResponseEntity.ok(new ApiResponse("Huérfanas obtenidas", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al obtener huérfanas", false, null));
        }
    }

    // PUT: Recibimos ambos IDs en la URL para poder hacer la vinculación manual
    @PutMapping("/vincular/{idImputacion}/{idDetalleEstimacion}")
    public ResponseEntity<ApiResponse> vincularManual(@PathVariable Long idImputacion, @PathVariable Long idDetalleEstimacion) {
        try {
            ImputacionClockify actualizada = service.vincularImputacionManual(idImputacion, idDetalleEstimacion);
            
            if (actualizada != null) {
                return ResponseEntity.ok(new ApiResponse("Tarea vinculada correctamente", true, actualizada));
            } else {
                return ResponseEntity.badRequest().body(new ApiResponse("La tarea no existe o ya estaba vinculada", false, null));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al procesar la solicitud", false, null));
        }
    }

    // GET: Obtener imputaciones por proyecto, tarea y departamento
    @GetMapping("/departamento/{idProyecto}/{idDetalleEstimacion}/{idDepartamento}")
    public ResponseEntity<ApiResponse> obtenerPorDepartamento(
            @PathVariable Long idProyecto, 
            @PathVariable Long idDetalleEstimacion, 
            @PathVariable Integer idDepartamento) {
        try {
            List<ImputacionClockify> lista = service.obtenerPorDepartamentoYDetalle(idProyecto, idDetalleEstimacion, idDepartamento);
            return ResponseEntity.ok(new ApiResponse("Imputaciones del departamento obtenidas", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al obtener imputaciones del departamento", false, null));
        }
    }

    // GET: Obtener cantidad de válidas por departamento
    @GetMapping("/departamento/{idProyecto}/{idDetalleEstimacion}/{idDepartamento}/validas/count")
    public ResponseEntity<ApiResponse> contarValidasDepartamento(
            @PathVariable Long idProyecto, 
            @PathVariable Long idDetalleEstimacion, 
            @PathVariable Integer idDepartamento) {
        try {
            Integer total = service.contarValidasPorDepartamento(idProyecto, idDetalleEstimacion, idDepartamento);
            return ResponseEntity.ok(new ApiResponse("Recuento de válidas", true, total));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al contar válidas", false, 0));
        }
    }

    // GET: Obtener cantidad de inválidas por departamento
    @GetMapping("/departamento/{idProyecto}/{idDetalleEstimacion}/{idDepartamento}/invalidas/count")
    public ResponseEntity<ApiResponse> contarInvalidasDepartamento(
            @PathVariable Long idProyecto, 
            @PathVariable Long idDetalleEstimacion, 
            @PathVariable Integer idDepartamento) {
        try {
            Integer total = service.contarInvalidasPorDepartamento(idProyecto, idDetalleEstimacion, idDepartamento);
            return ResponseEntity.ok(new ApiResponse("Recuento de inválidas", true, total));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al contar inválidas", false, 0));
        }
    }
}