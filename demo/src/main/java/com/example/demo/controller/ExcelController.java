package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.services.DetalleEstimacionService;
import com.example.demo.services.ExcelService;

@RestController
@RequestMapping("/api/estimaciones")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private DetalleEstimacionService detalleEstimacionService;

    @GetMapping("/exportar/{proyectoId}")
    public ApiResponse exportarExcel(@PathVariable Long proyectoId) {

        List<DetalleEstimacion> estimaciones = detalleEstimacionService.obtenerEstimacionesPorProyecto(proyectoId);

        if (estimaciones == null || estimaciones.isEmpty()) {
            return new ApiResponse("Error: El proyecto " + proyectoId + " no tiene estimaciones guardadas.", false, null);
        }

        int filasGuardadas = excelService.crearYGuardarExcel(estimaciones);

        if (filasGuardadas == 0) {
            return new ApiResponse("Error al crear el archivo Excel.", false, null);
        }

        if (filasGuardadas > 0) {
            return new ApiResponse("Éxito: Se ha generado el Excel del proyecto " + proyectoId + " con " + filasGuardadas + " estimaciones.", true, filasGuardadas);
        }

        return new ApiResponse("Error: Estado de la operación desconocido.", false, null);
    }
}