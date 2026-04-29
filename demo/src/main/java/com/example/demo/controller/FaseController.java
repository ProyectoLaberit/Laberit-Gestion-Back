package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.HistorialExcelDTO;
import com.example.demo.services.ExcelService;
import com.example.demo.services.FaseService;

@RestController
@RequestMapping("/api/fases")
@CrossOrigin(origins = "*")
public class FaseController {

    @Autowired
    private FaseService faseService;

    @Autowired
    private ExcelService excelService;

    /**
     * Devuelve las fases/subfases del Excel VIGENTE de un proyecto.
     */
    @GetMapping("/{idProyecto}")
    public ApiResponse obtenerJerarquiaFases(@PathVariable Long idProyecto) {
        try {
            List<FaseDTO> jerarquia = faseService.obtenerJerarquiaFasesPorProyecto(idProyecto);

            if (jerarquia.isEmpty()) {
                return new ApiResponse("El proyecto no tiene tareas o excel activo", true, jerarquia);
            }

            return new ApiResponse("Jerarquía de fases activa recuperada", true, jerarquia);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar jerarquía: " + e.getMessage(), false, null);
        }
    }

    /**
     * Devuelve las fases/subfases de un Excel CONCRETO (por su idExcel).
     * Usado para el historial: al seleccionar un excel del desplegable se cargan sus fases.
     */
    @GetMapping("/por-excel/{idExcel}")
    public ApiResponse obtenerJerarquiaPorExcel(@PathVariable Integer idExcel) {
        try {
            List<FaseDTO> jerarquia = faseService.obtenerJerarquiaPorIdExcel(idExcel);

            if (jerarquia.isEmpty()) {
                return new ApiResponse("Este excel no tiene tareas registradas", true, jerarquia);
            }

            return new ApiResponse("Jerarquía recuperada para excel " + idExcel, true, jerarquia);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar jerarquía: " + e.getMessage(), false, null);
        }
    }

    /**
     * Devuelve el historial de excels subidos para un proyecto, ordenados del más reciente al más antiguo.
     */
    @GetMapping("/historial/{idProyecto}")
    public ApiResponse obtenerHistorialExcels(@PathVariable Long idProyecto) {
        try {
            List<HistorialExcelDTO> historial = excelService.obtenerHistorialExcels(idProyecto);
            return new ApiResponse("Historial de excels recuperado", true, historial);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar historial: " + e.getMessage(), false, null);
        }
    }
}
