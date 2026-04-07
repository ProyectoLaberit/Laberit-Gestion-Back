package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.DetalleEstimacion;
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
    /* * @PathVariable: Captura el valor que viene en la URL (idProyecto)
     * y lo pasa como argumento al método.
     * La ruta será: http://localhost:8080/api/detalle-estimacion/proyecto/4
     */
    public List<DetalleEstimacion> obtenerTareasPorProyecto(@PathVariable Long idProyecto) {
        // Se utiliza el método del repositorio para obtener solo las tareas vinculadas
        return detalleEstimacionRepository.findByIdProyecto(idProyecto);
    }

   @PostMapping("/importar")
    public ApiResponse importarExcel(
            @RequestParam("archivo") MultipartFile archivo, 
            @RequestParam("proyectoId") long proyectoId, 
            @RequestParam("usuarioId") Integer usuarioId) {

        if (archivo.isEmpty()) {
            return new ApiResponse("El archivo está vacío.", false, null);
        }

        try {
            int filasGuardadas = detalleEstimacionService.procesarExcel(archivo, proyectoId, usuarioId);
            return new ApiResponse("Éxito: se registraron " + filasGuardadas + " estimaciones.", true, filasGuardadas);
        } catch (Exception e) {
            return new ApiResponse("Error al procesar: " + e.getMessage(), false, null);
        }
    }

    
    @GetMapping("/exportar/{proyectoId}")
    public ApiResponse exportarExcel(@PathVariable Long proyectoId) {
        
        List<DetalleEstimacion> estimaciones = detalleEstimacionService.obtenerEstimacionesPorProyecto(proyectoId);

        if (estimaciones == null || estimaciones.isEmpty()) {
            return new ApiResponse("No hay estimaciones para el proyecto " + proyectoId, false, null);
        }

        int total = excelService.crearYGuardarExcel(estimaciones);
        return new ApiResponse("Listado de estimaciones obtenido.", true, total);
    }
}