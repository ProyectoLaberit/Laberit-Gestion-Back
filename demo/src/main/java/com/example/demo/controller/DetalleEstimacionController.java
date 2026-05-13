package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.DetalleEstimacionDTO;
import com.example.demo.dto.HistorialExcelDTO;
import com.example.demo.dto.ResumenTiemposDTO;
import com.example.demo.dto.TareaSubfaseDTO;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.services.DetalleEstimacionService;
import com.example.demo.services.ExcelService;
import com.example.demo.services.AuditService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/estimaciones")
@CrossOrigin(origins = "*")
public class DetalleEstimacionController {

   

    @Autowired
    private DetalleEstimacionService detalleEstimacionService;

    @Autowired
    private ExcelService excelService;

   
    
    /**
     * Recupera la tabla de estimaciones del Excel VIGENTE (el más reciente o activo) de un proyecto.
     * El Frontend debe usar este endpoint por defecto para ver el estado actual.
     * @param idProyecto ID del proyecto a consultar.
     * @return ApiResponse con la lista de DetalleEstimacionDTO.
     */
    @GetMapping("/proyecto/{idProyecto}")
    public ApiResponse obtenerTareasPorProyecto(@PathVariable Long idProyecto) {
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);

        if (excel == null) {
            return new ApiResponse("El proyecto no tiene estimaciones subidas", true, java.util.List.of());
        }

        List<DetalleEstimacionDTO> lista = detalleEstimacionService.obtenerDetallesPorExcel(excel.getIdExcel());
        return new ApiResponse("Listado de tareas recuperado", true, lista);
    }


    /**
         * Procesa y guarda la matriz de estimaciones desde un archivo Excel.
         * @param archivo El archivo .xlsx cargado desde el cliente.
         * @param proyectoId ID del proyecto al que se asocia el Excel.
         * @param usuarioId ID del usuario que realiza la subida.
         * @return ApiResponse con el número total de registros importados.
    */
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


    /**
         * Actualiza los valores de una tarea de estimación existente.
         * @param id ID único del registro en la base de datos.
         * @param detalleDTO Objeto con los nuevos valores (tarea, tiempos, fase, depto).
         * @return ApiResponse confirmando la actualización.
    */
  @PutMapping("/{id}")
    public ApiResponse actualizarDetalle(@PathVariable Long id, @RequestBody DetalleEstimacionDTO detalleDTO) {
        try {
            detalleEstimacionService.actualizarDetalle(id, detalleDTO);
            return new ApiResponse("Tarea actualizada correctamente", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al actualizar: " + e.getMessage(), false, null);
        }
    }

    /**
        * Genera un nuevo archivo Excel basado en las estimaciones guardadas en la BD.
        * @param idProyecto ID del proyecto cuyas estimaciones se quieren exportar.
        * @return ApiResponse indicando si el archivo se generó correctamente.
     */
    @GetMapping("/exportar/{idProyecto}")
    public ApiResponse exportarExcel(@PathVariable Long idProyecto) {
        Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);

        if (excel == null) {
            return new ApiResponse("Error: El proyecto " + idProyecto + " no tiene un Excel asociado.", false, null);
        }
        
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

    /**
         * Busca una estimación puntual filtrando por proyecto, subfase y nombre de tarea.
         * @param idProyecto ID del proyecto.
         * @param subfase Nombre de la subfase (ej. "investigacion").
         * @param tarea Nombre de la tarea (ej. "Benchmark").
         * @return ApiResponse con el DTO de la estimación encontrada.
    */
    @PostMapping("/proyecto/{idProyecto}/especifica")
    public ApiResponse obtenerEstimacionEspecifica(
        @PathVariable Long idProyecto,
        @RequestParam Integer idSubfase,
        @RequestParam String tarea,
        @RequestParam(required = false) Integer idExcelElegido) {
    
    // Ahora recibimos una lista de DTOs con cada departamento y sus tiempos
    List<DetalleEstimacionDTO> detalles = detalleEstimacionService.obtenerDetallePorCriterios(idProyecto, idSubfase, tarea, idExcelElegido);

    if (detalles.isEmpty()) {
        return new ApiResponse("No se encontraron registros para esta tarea y subfase", false, detalles);
    }

    return new ApiResponse("Desglose por departamentos recuperado con éxito", true, detalles);
}

    /**
     * Recupera la tabla de estimaciones de un Excel ESPECÍFICO por su ID.
     * Ideal para consultar el historial, auditorías o versiones antiguas de una estimación.
     * @param idExcel ID exacto del Excel cuyas estimaciones se quieren recuperar.
     * @return ApiResponse con la lista de DetalleEstimacionDTO con nombres legibles.
     */
    @GetMapping("/excel/{idExcel}")
    public ApiResponse obtenerEstimacionesPorExcel(@PathVariable Integer idExcel) {
        try {
            List<DetalleEstimacionDTO> detalles = detalleEstimacionService.obtenerDetallesPorExcel(idExcel);
            
            if (detalles.isEmpty()) {
                return new ApiResponse("No hay estimaciones para este Excel", true, detalles);
            }
            
            return new ApiResponse("Estimaciones recuperadas con éxito", true, detalles);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar la tabla: " + e.getMessage(), false, null);
        }
    }
    /**
     * Endpoint para obtener todas las tareas de una subfase con sus estimaciones totales.
     * Recibe un JSON por POST con idProyecto e idSubfase.
     */
    @PostMapping("/subfase/tareas") 
    public ApiResponse obtenerTareasSubfase(
            @RequestParam Long idProyecto, 
            @RequestParam Integer idSubfase,
            @RequestParam(required = false) Integer idExcelElegido) {
            
        try {
            // Llamamos directamente a tu lógica pasándole los dos IDs
            List<TareaSubfaseDTO> tareas = detalleEstimacionService.obtenerTareasSubfase(idProyecto, idSubfase,idExcelElegido);
            
            if (tareas.isEmpty()) {
                return new ApiResponse("No hay tareas para esta subfase o el proyecto no tiene Excel activo", true, tareas);
            }
            
            return new ApiResponse("Tareas recuperadas con éxito", true, tareas);
            
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar las tareas: " + e.getMessage(), false, null);
        }
    }
    /**
     * Recupera el historial de archivos Excel subidos a un proyecto específico.
     * Permite al Frontend listar todas las versiones disponibles, indicando quién lo subió y si es la versión vigente.
     * @param id ID del proyecto a consultar.
     * @return ApiResponse con la lista de HistorialExcelDTO ordenada por fecha de subida.
     */
    @GetMapping("/{id}/historial-excels")
    public ApiResponse obtenerHistorialExcels(@PathVariable("id") Long id) {
    // Llamamos al servicio que ya usa la query del repositorio
    List<HistorialExcelDTO> historial = excelService.obtenerHistorialExcels(id);
    
    return new ApiResponse("Historial recuperado con éxito", true, historial);
}


    /**
     * Crea una nueva tarea de estimación manualmente desde el frontend.
     * Recibe el idExcel vigente, idFase (subfase), idDepartamento, nombre de tarea, tiempoMin y tiempoMax.
     */
    @PostMapping
    public ApiResponse crearTarea(@RequestBody DetalleEstimacionDTO dto) {
        try {
            detalleEstimacionService.crearTarea(dto);
            return new ApiResponse("Tarea creada correctamente.", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al crear la tarea: " + e.getMessage(), false, null);
        }
    }

    /**
     * Obtiene los resúmenes de tiempos de TODAS las subfases de un proyecto de golpe.
     */
    @GetMapping("/resumen/subfases/{idProyecto}")
    public ApiResponse obtenerResumenTodasSubfases(@PathVariable Long idProyecto) {
        try {
            Map<Integer, ResumenTiemposDTO> resumenes = detalleEstimacionService.obtenerResumenTodasSubfases(idProyecto);
            return new ApiResponse("Resumen masivo recuperado", true, resumenes);
        } catch (Exception e) {
            return new ApiResponse("Error al calcular resumen masivo: " + e.getMessage(), false, null);
        }
    }

    /**
     * Obtiene el resumen de tiempos de un proyecto completo.
     */
    @GetMapping("/resumen/proyecto/{idProyecto}")
    public ApiResponse obtenerResumenProyecto(@PathVariable Long idProyecto) {
        try {
            ResumenTiemposDTO resumen = detalleEstimacionService.obtenerResumenProyecto(idProyecto);
            return new ApiResponse("Resumen de proyecto recuperado", true, resumen);
        } catch (Exception e) {
            return new ApiResponse("Error al calcular resumen: " + e.getMessage(), false, null);
        }
    }

    /**
     * Obtiene los resúmenes de una lista de proyectos.
     */
    @PostMapping("/proyectos/resumen")
    public ApiResponse obtenerResumenProyectos(@RequestBody List<Long> idsProyectos) {
        try {
            Map<Long, ResumenTiemposDTO> resumenes = detalleEstimacionService.obtenerResumenVariosProyectos(idsProyectos);
            return new ApiResponse("Resúmenes de proyectos recuperados", true, resumenes);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar resúmenes", false, null);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarEstimacion(@PathVariable Long id) {
        try {
            detalleEstimacionService.eliminarTarea(id);
            return ResponseEntity.ok().body("{\"mensaje\": \"Estimación eliminada correctamente\", \"success\": true}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"mensaje\": \"" + e.getMessage() + "\", \"success\": false}");
        }
    }
} // <-- fin del controlador -->
