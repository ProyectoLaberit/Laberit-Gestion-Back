package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.DetalleEstimacionDTO;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.services.DetalleEstimacionService;
import com.example.demo.services.ExcelService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/estimaciones")
@CrossOrigin(origins = "*")
public class DetalleEstimacionController {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private DetalleEstimacionService detalleEstimacionService;

    @Autowired
    private ExcelService excelService;
    
    @GetMapping("/proyecto/{idProyecto}")
    public ApiResponse obtenerTareasPorProyecto(@PathVariable Long idProyecto) {
        // 1. Buscamos el Excel asociado a este proyecto
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);

        if (excel == null) {
            // Si no hay Excel, devolvemos una lista vacía sin dar error 500
            return new ApiResponse("El proyecto no tiene estimaciones subidas", true, java.util.List.of());
        }

        // 2. Si existe, usamos su ID para buscar las estimaciones
        List<DetalleEstimacionDTO> lista = detalleEstimacionService.obtenerDetallesPorExcel(excel.getIdExcel());
        
        return new ApiResponse("Listado de tareas recuperado", true, lista);
    }

    @PostMapping("/importar")
    public ApiResponse importarExcel(@RequestParam("archivo") MultipartFile archivo, @RequestParam("proyectoId") long proyectoId, @RequestParam("usuarioId") Integer usuarioId) {
        
        if(archivo.isEmpty()) {
            return new ApiResponse("El archivo está vacío.", false, null);
        }

        try {
            int filasGuardadas = detalleEstimacionService.procesarExcel(archivo, proyectoId, usuarioId);
            return new ApiResponse("Éxito: se importaron " + filasGuardadas + " registros.", true, filasGuardadas);
        } catch (Exception e) {
            return new ApiResponse("Error al procesar el Excel: " + e.getMessage(), false, null);
        }
    }

    @PutMapping("/{id}")
    public ApiResponse actualizarDetalle(@PathVariable Long id, @RequestBody DetalleEstimacionDTO detalleDTO) {
        try {
            // Buscamos el detalle. Si no existe, lanza una excepción que atrapa el 'catch'
            DetalleEstimacion detalle = detalleEstimacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la tarea con ID: " + id));

            // Si lo encuentra, actualizamos los campos
            detalle.setTarea(detalleDTO.getTarea());
            detalle.setIdDepartamento(detalleDTO.getIdDepartamento());
            detalle.setIdFase(detalleDTO.getIdFase());
            detalle.setTiempoMax(detalleDTO.getTiempoMax());
            detalle.setTiempoMin(detalleDTO.getTiempoMin());

            // Guardamos los cambios (esto hace el UPDATE en la base)
            detalleEstimacionRepository.save(detalle);

            return new ApiResponse("Tarea actualizada correctamente", true, null);

        } catch (Exception e) {
            return new ApiResponse("Error al actualizar: " + e.getMessage(), false, null);
        }
    }
    
    // CORRECCIÓN AQUÍ: {idProyecto} coincide exactamente con @PathVariable Long idProyecto
    @GetMapping("/exportar/{idProyecto}")
    public ApiResponse exportarExcel(@PathVariable Long idProyecto) {
        // 1. Buscamos el Excel
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);

        if (excel == null) {
            return new ApiResponse("Error: El proyecto " + idProyecto + " no tiene un Excel asociado.", false, null);
        }
        
        // 2. Buscamos las entidades
        List<DetalleEstimacion> estimaciones = detalleEstimacionService.obtenerDetallesEntidadPorExcel(excel.getIdExcel());

        if (estimaciones == null || estimaciones.isEmpty()) {
            return new ApiResponse("Error: El Excel está vacío.", false, null);
        }

        int resultado = excelService.crearYGuardarExcel(estimaciones);

        if (resultado > 0) {
            return new ApiResponse("Éxito: Excel generado correctamente", true, resultado);
        }

        return new ApiResponse("Error al generar el archivo.", false, null);
    }
}