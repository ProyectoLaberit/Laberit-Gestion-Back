package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.DetalleEstimacionDTO;
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
    public ApiResponse obtenerTareasPorProyecto(@PathVariable Long idProyecto) {
        // Llamamos al servicio (que ya mapea las entidades a DTOs)
        List<DetalleEstimacionDTO> lista = detalleEstimacionService.obtenerDetallesPorProyecto(idProyecto);
        
        // Devolvemos el formato estándar
        return new ApiResponse("Listado de tareas del proyecto recuperado", true, lista);
    }

    @PostMapping("/importar")
    public ApiResponse importarExcel(@RequestParam("archivo") MultipartFile archivo, @RequestParam("proyectoId") long proyectoId, @RequestParam("usuarioId") Integer usuarioId) {
        
        if(archivo.isEmpty()) {
            return  new ApiResponse("El archivo está vacío.", false, null);

        }

        try {
            int filasGuardadas = detalleEstimacionService.procesarExcel(archivo, proyectoId, usuarioId);
            return new ApiResponse("Éxito: se importaron " + filasGuardadas + " registros.", true, filasGuardadas);
        } catch (Exception e) {
            return new ApiResponse("Error al procesar el Excel: " + e.getMessage(), false, null);
        }
    }

    //La ruta del front
    @PutMapping("/{id}")

    //@PathVariable: Saca el dato de la url, en este caso la id que se especifica con PutMapping

    //@RequestBody: Recibe un objeto completo con los datos del proyecto con esa id
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
    
    @GetMapping("/exportar/{proyectoId}")
    public ApiResponse exportarExcel(@PathVariable Long proyectoId) {
        
        List<DetalleEstimacion> estimaciones = detalleEstimacionService.obtenerDetallesEntidadPorProyecto(proyectoId);

        if (estimaciones == null || estimaciones.isEmpty()) {
            return new ApiResponse("Error: El proyecto " + proyectoId + " no tiene estimaciones.", false, null);
        }

        int resultado = excelService.crearYGuardarExcel(estimaciones);

        if (resultado > 0) {
            return new ApiResponse("Éxito: Excel generado para el proyecto " + proyectoId, true, resultado);
        }

        return new ApiResponse("Error al generar el archivo.", false, null);
    }
}