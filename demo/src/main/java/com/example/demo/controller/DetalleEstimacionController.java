package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.services.DetalleEstimacionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


import java.util.List;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/estimaciones")
@CrossOrigin(origins = "*")
public class DetalleEstimacionController {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @Autowired
    private DetalleEstimacionService detalleEstimacionService;
    
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
    public ApiResponse importarExcel(@RequestParam("archivo") MultipartFile archivo, @RequestParam("proyectoId") long proyectoId) {
        
        if(archivo.isEmpty()) {
          return  new ApiResponse("El archivo está vacío.", false, null);
           
        }
        try{
            detalleEstimacionService.procesarExcell(archivo, proyectoId);
            return new ApiResponse("Archivo procesado exitosamente.", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al procesar el archivo: " + e.getMessage(), false, null);
        }
        
        
    }
    
}