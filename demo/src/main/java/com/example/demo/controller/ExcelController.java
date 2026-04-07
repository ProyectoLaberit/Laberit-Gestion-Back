package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/exportar")
    public ApiResponse exportarExcel() {
        
        List<DetalleEstimacion> estimaciones = detalleEstimacionService.obtenerTodasLasEstimaciones();

        int filasGuardadas = excelService.crearYGuardarExcel(estimaciones);

        if (filasGuardadas == 0) {
            return new ApiResponse("Error: No hay datos en la base de datos para exportar o falló la creación.", false, null);
        }
        
        if (filasGuardadas > 0) {
            return new ApiResponse("Éxito: Se ha generado el Excel con " + filasGuardadas + " estimaciones.", true, filasGuardadas);
        } 
        
        return new ApiResponse("Error: Estado de la operación desconocido.", false, null);
        
    }
}
