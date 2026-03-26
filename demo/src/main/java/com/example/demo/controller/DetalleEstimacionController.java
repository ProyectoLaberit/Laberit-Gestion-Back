package com.example.demo.controller;

import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.repository.DetalleEstimacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detalle_estimacion")
@CrossOrigin(origins = "*")
public class DetalleEstimacionController {

    @Autowired
    private DetalleEstimacionRepository detalleEstimacionRepository;

    @GetMapping("/proyecto/{idProyecto}")
    /* * @PathVariable: Captura el valor que viene en la URL (idProyecto)
     * y lo pasa como argumento al método.
     * La ruta será: http://localhost:8080/api/detalle-estimacion/proyecto/4
     */
    public List<DetalleEstimacion> obtenerTareasPorProyecto(@PathVariable Long idProyecto) {
        // Se utiliza el método del repositorio para obtener solo las tareas vinculadas
        return detalleEstimacionRepository.findByIdProyecto(idProyecto);
    }
}