package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.DetalleEstimacionDTO;
import com.example.demo.dto.HistorialExcelDTO;
import com.example.demo.dto.ResumenTiemposDTO;
import com.example.demo.dto.TareaSubfaseDTO;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.services.DetalleEstimacionService;
import com.example.demo.services.ExcelService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

    private boolean esAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }

        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> { return a.equals("ROLE_ADMINISTRADOR") || a.equals("ROLE_SUPERADMINISTRADOR"); });
    }
    
    /**
     * Recupera la tabla de estimaciones del Excel VIGENTE (el más reciente o
     * activo) de un proyecto.
     * El Frontend debe usar este endpoint por defecto para ver el estado actual.
     * 
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
        if (archivo.isEmpty()) {
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
     * 
     * @param id         ID único del registro en la base de datos.
     * @param detalleDTO Objeto con los nuevos valores (tarea, tiempos, fase,
     *                   depto).
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

        List<DetalleEstimacion> estimaciones = detalleEstimacionService
                .obtenerDetallesEntidadPorExcel(excel.getIdExcel());

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
     * @param idSubfase ID de la subfase.
     * @param tarea Nombre de la tarea (ej. "Benchmark").
     * @param idExcelElegido ID opcional del Excel si se mira el historial.
     * @return ApiResponse con el DTO de la estimación encontrada.
     */
    @PostMapping("/proyecto/{idProyecto}/especifica")
    public ApiResponse obtenerEstimacionEspecifica(
        @PathVariable Long idProyecto,
        @RequestParam Integer idSubfase,
        @RequestParam String tarea,
        @RequestParam(required = false) Integer idExcelElegido) {
    
        List<DetalleEstimacionDTO> detalles = detalleEstimacionService.obtenerDetallePorCriteriosHistorico(idProyecto, idSubfase, tarea, idExcelElegido);

        if (detalles.isEmpty()) {
            return new ApiResponse("No se encontraron registros para esta tarea y subfase", false, detalles);
        }

        return new ApiResponse("Desglose por departamentos recuperado con éxito", true, detalles);
    }

    /**
     * Recupera la tabla de estimaciones de un Excel ESPECÍFICO por su ID.
     * Ideal para consultar el historial, auditorías o versiones antiguas de una
     * estimación.
     * 
     * @param idExcel ID exacto del Excel cuyas estimaciones se quieren recuperar.
     * @return ApiResponse con la lista de DetalleEstimacionDTO con nombres
     *         legibles.
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
     * Metodo para obtener todas las tareas de una subfase con sus estimaciones
     * totales
     * 
     * @param idProyecto     id del proyecto del que obtenemos la informacion
     * @param idSubfase      id de la subfase de la que obtenemos las tareas
     * @param idExcelElegido id del excel del que queremos obtener los tiempos,
     *                       puede estar vacio
     * @return ApiResponse json que contiene todas las tareas de la subfase con sus
     *         estimaciones totales
     */
    @PostMapping("/subfase/tareas")
    public ApiResponse obtenerTareasSubfase(
            @RequestParam Long idProyecto,
            @RequestParam Integer idSubfase,
            @RequestParam(required = false) Integer idExcelElegido) {

        try {
            List<TareaSubfaseDTO> tareas = detalleEstimacionService.obtenerTareasSubfaseHistorico(idProyecto, idSubfase, idExcelElegido);
            
            if (tareas.isEmpty()) {
                return new ApiResponse("No hay tareas para esta subfase o el proyecto no tiene Excel activo", true,
                        tareas);
            }

            return new ApiResponse("Tareas recuperadas con éxito", true, tareas);

        } catch (Exception e) {
            return new ApiResponse("Error al recuperar las tareas: " + e.getMessage(), false, null);
        }
    }

    /**
     * Recupera el historial de archivos Excel subidos a un proyecto específico.
     * Permite al Frontend listar todas las versiones disponibles, indicando quién
     * lo subió y si es la versión vigente.
     * 
     * @param id ID del proyecto a consultar.
     * @return ApiResponse con la lista de HistorialExcelDTO ordenada por fecha de
     *         subida.
     */
    @GetMapping("/{id}/historial-excels")
    public ApiResponse obtenerHistorialExcels(@PathVariable("id") Long id) {
        List<HistorialExcelDTO> historial = excelService.obtenerHistorialExcels(id);
        return new ApiResponse("Historial recuperado con éxito", true, historial);
    }

    /**
     * Metodo para crear una nueva tarea
     * 
     * @param dto objeto tipo DetalleEstimacionDTO con los datos de la tarea a crear
     * @return ApiResponse con un booleano a true si la creacion a tenido exito y
     *         false si hubo algun problema
     */
    @PostMapping("/tarea")
    public ApiResponse crearTarea(@RequestBody DetalleEstimacionDTO dto) {
        if (!esAdmin()) {
            return new ApiResponse("No tienes permisos para realizar esta accion.", false, null);
        }

        try {
            detalleEstimacionService.crearTarea(dto);
            return new ApiResponse("Tarea creada correctamente.", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al crear la tarea: " + e.getMessage(), false, null);
        }
    }

    /**
     * Crea una nueva estimacion manual inyectándola directamente en el Excel vigente del proyecto.
     * Este es el endpoint que usa crearfases.html.
     */
    /**
     * Metodo para crear una tarea manualmente para el excel vigente del proyecto
     * @param idProyecto id del proyecto al que pertenece la tarea
     * @param dto objeto tipo DetalleEstimacionDTO que contiene la informacion de la tarea a crear
     * @return ApiResponse con un boolean a true si la creacion ha tenido exito y false si no
     */
    @PostMapping("/proyecto/{idProyecto}/manual")
    public ApiResponse crearTareaManual(@PathVariable Long idProyecto, @RequestBody DetalleEstimacionDTO dto) {
        if (!esAdmin()) {
            return new ApiResponse("No tienes permisos para realizar esta accion.", false, null);
        }

        try {
            Excel excel = excelService.obtenerExcelVigentePorProyecto(idProyecto);
            if (excel == null) {
                return new ApiResponse("El proyecto no tiene un Excel vigente asociado.", false, null);
            }

            dto.setIdExcel(excel.getIdExcel());
            detalleEstimacionService.crearTarea(dto);
            return new ApiResponse("Tarea creada correctamente.", true, null);
        } catch (Exception e) {
            return new ApiResponse("Error al crear la tarea: " + e.getMessage(), false, null);
        }
    }
    /**
     * Metodo para obtener los resumenes de todas las subfases de un proyecto
     * @param idProyecto id del proyecto del que obtenemos el resumen de tiempos
     * @param idExcelElegido id del excel del que queremos obtener el resumen, opcional
     * @return ApiResponse json que contiene el resumen de los tiempos de todas las subfases del proyecto 
     */
    @GetMapping("/resumen/subfases/{idProyecto}")
    public ApiResponse obtenerResumenTodasSubfases(
            @PathVariable Long idProyecto,
            @RequestParam(required = false) Integer idExcelElegido) {
        try {
            Map<Integer, ResumenTiemposDTO> resumenes = detalleEstimacionService
                    .obtenerResumenTodasSubfasesHistorico(idProyecto, idExcelElegido);
            return new ApiResponse("Resumen masivo recuperado", true, resumenes);
        } catch (Exception e) {
            return new ApiResponse("Error al calcular resumen masivo: " + e.getMessage(), false, null);
        }
    }
    /**
     * Metodo para obtener el resumen de tiempos de un proyecto completo
     * @param idProyecto id del proyecto del que queremos obtener el resumen
     * @return ApiResponse json que contiene lso tiempos totales del proyecto
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
     * Metodo que obtiene los resumenes de tiempo de una lista de proyectos
     * @param idsProyectos Lista de ids de poryectos de los que obtener los resumenes
     * @return ApiResponse json que contiene la lista de resumenes de los tiempos de los proyectos solicitados
     */
    @PostMapping("/proyectos/resumen")
    public ApiResponse obtenerResumenProyectos(@RequestBody List<Long> idsProyectos) {
        try {
            Map<Long, ResumenTiemposDTO> resumenes = detalleEstimacionService
                    .obtenerResumenVariosProyectos(idsProyectos);
            return new ApiResponse("Resúmenes de proyectos recuperados", true, resumenes);
        } catch (Exception e) {
            return new ApiResponse("Error al recuperar resúmenes", false, null);
        }
    }

    /**
     * Elimina una estimación por su ID. Estandarizado a ApiResponse.
     */
    @DeleteMapping("/{id}")
    public ApiResponse eliminarEstimacion(@PathVariable Long id) {
        try {
            detalleEstimacionService.eliminarTarea(id);
            return new ApiResponse("Estimación eliminada correctamente", true, null);
        } catch (Exception e) {
            return new ApiResponse(e.getMessage(), false, null);
        }
    }
}