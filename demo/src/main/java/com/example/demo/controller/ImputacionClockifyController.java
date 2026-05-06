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

    // GET: Suma de tiempos de tarea/departamento
    @GetMapping("/suma/{idDetalleEstimacion}")
    public ResponseEntity<ApiResponse> obtenerSumaHoras(@PathVariable Long idDetalleEstimacion) {
        try {
            Double suma = service.obtenerSumaHorasValidas(idDetalleEstimacion);
            return ResponseEntity.ok(new ApiResponse("Suma obtenida correctamente", true, suma));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al calcular la suma", false, null));
        }
    }

    // GET: Para rellenar la tabla de imputaciones
    @GetMapping("/validas/{idDetalleEstimacion}")
    public ResponseEntity<ApiResponse> obtenerValidas(@PathVariable Long idDetalleEstimacion) {
        try {
            List<ImputacionClockify> lista = service.obtenerImputacionesValidas(idDetalleEstimacion);
            return ResponseEntity.ok(new ApiResponse("Lista de imputaciones obtenida", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al obtener imputaciones", false, null));
        }
    }

    // CAMBIO AQUÍ: Ahora buscamos las huérfanas por idProyecto
    @GetMapping("/huerfanas/{idProyecto}")
    public ResponseEntity<ApiResponse> obtenerHuerfanas(@PathVariable Long idProyecto) {
        try {
            List<ImputacionClockify> lista = service.obtenerHuerfanas(idProyecto);
            return ResponseEntity.ok(new ApiResponse("Huérfanas obtenidas", true, lista));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse("Error al obtener huérfanas", false, null));
        }
    }

    // CAMBIO AQUÍ: Recibimos ambos IDs en la URL para poder hacer la vinculación
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
}