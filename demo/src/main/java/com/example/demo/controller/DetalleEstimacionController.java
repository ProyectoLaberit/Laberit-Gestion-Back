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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
    
    /**
        * Recupera todas las tareas asociadas al Excel vigente de un proyecto.
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
            DetalleEstimacion detalle = detalleEstimacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No se encontró la tarea con ID: " + id));

            detalle.setTarea(detalleDTO.getTarea());
            detalle.setIdDepartamento(detalleDTO.getIdDepartamento());
            detalle.setIdFase(detalleDTO.getIdFase());
            detalle.setTiempoMax(detalleDTO.getTiempoMax());
            detalle.setTiempoMin(detalleDTO.getTiempoMin());

            detalleEstimacionRepository.save(detalle);

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
 @GetMapping("/proyecto/{idProyecto}/especifica")
    public ApiResponse obtenerEstimacionEspecifica(
            @PathVariable Long idProyecto,
            @RequestParam String fase,
            @RequestParam String subfase,
            @RequestParam String tarea) {
        
        DetalleEstimacionDTO estimacion = detalleEstimacionService.obtenerDetallePorCriterios(idProyecto, fase, subfase, tarea);

        if (estimacion == null) {
            return new ApiResponse("No se encontró la estimación para esa jerarquía exacta.", false, null);
        }

        return new ApiResponse("Estimación recuperada con éxito", true, estimacion);
    }
    
}