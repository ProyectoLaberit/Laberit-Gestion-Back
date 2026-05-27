package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.GitLabProyectoDTO;
import com.example.demo.dto.GitLabTareaDTO; // [NUEVO] Importamos el nuevo DTO
import com.example.demo.entity.GitLabTarea;
import com.example.demo.services.GitLabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gitlab")
@CrossOrigin(origins = "*")
public class GitLabController {

    @Autowired
    private GitLabService gitLabService;

    /**
     * Endpoint para obtener el listado de tareas vinculadas a un proyecto de
     * GitLab.
     * * Consulta la configuración del proyecto en la base de datos local y utiliza
     * el
     * Token Maestro para recuperar los Issues/Work Items directamente desde la API
     * v4 de GitLab.
     *
     * @param proyectoId Identificador único del proyecto en el sistema local.
     * @return ResponseEntity con ApiResponse que contiene:
     *         - 200 OK: Si la conexión fue exitosa y se recuperaron los datos.
     *         - 500 Internal Server Error: Si hubo un fallo en la comunicación con
     *         GitLab o error de configuración.
     */
    @GetMapping("/sincronizar/{proyectoId}")
    public ResponseEntity<ApiResponse> sincronizarTareasGitLab(@PathVariable Long proyectoId) {
        try {
            int tareasNuevas = gitLabService.sincronizarYContarNuevasTareas(proyectoId);

            return ResponseEntity
                    .ok(new ApiResponse("Tareas recuperadas de GitLab: " + tareasNuevas, false, tareasNuevas));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    /**
     * Obtiene los proyectos de GitLab que aún no han sido vinculados en el sistema.
     * * Compara la lista total de proyectos accesibles mediante el Token Maestro
     * contra los registros existentes en la tabla 'proyectos'. Este filtrado
     * evita la duplicidad de registros al permitir al usuario importar solo
     * proyectos nuevos.
     *
     * @return ResponseEntity con ApiResponse que contiene:
     *         - 200 OK: Lista recuperada (puede estar vacía si todos ya están
     *         registrados).
     *         - 500 Internal Server Error: Error de comunicación con la API de
     *         GitLab.
     */
    @GetMapping("/externos")
    public ResponseEntity<ApiResponse> getProyectosExternos() {
        try {
            List<GitLabProyectoDTO> proyectosExternos = gitLabService.obtenerProyectosGitLabNoRegistrados();
            return ResponseEntity.ok(new ApiResponse("Proyectos externos recuperados", true, proyectosExternos));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse(e.getMessage(), false, null));
        }
    }

    // =========================================================================
    // [NUEVOS ENDPOINTS] Mantenimiento de vinculaciones locales en Neon
    // =========================================================================

    /**
     * Vincula una issue de GitLab a una tarea del proyecto real.
     */
    @PostMapping("/vincular")
    public ResponseEntity<ApiResponse> vincularTarea(
            @RequestBody GitLabTareaDTO tareaDTO,
            @RequestParam Long idTareaProyecto,
            @RequestParam(required = false) String urlProyecto) {
        try {
            GitLabTarea vinculada = gitLabService.vincularTareaAProyecto(tareaDTO, idTareaProyecto);
            return ResponseEntity.ok(new ApiResponse("Tarea vinculada con éxito", true, vinculada));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al vincular tarea: " + e.getMessage(), false, null));
        }
    }

    /**
     * VISTA 1: Obtiene únicamente las tareas confirmadas y emparejadas (valida =
     * true).
     */
    @GetMapping("/vinculadas/validas/{idProyecto}")
    public ResponseEntity<ApiResponse> getSoloValidadas(@PathVariable Long idProyecto) {
        try {
            // Llamamos a la función de lectura limpia de Neon
            List<GitLabTarea> validas = gitLabService.obtenerSoloValidasDeBaseDatos(idProyecto);

            return ResponseEntity.ok(
                    new ApiResponse("Tareas válidas recuperadas de Neon con éxito", true, validas));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al leer válidas: " + e.getMessage(), false, null));
        }
    }

    /**
     * VISTA 2: Obtiene el listado completo de control (Válidas +
     * Inválidas/Huérfanas).
     */
    @GetMapping("/vinculadas/todas/{idProyecto}")
    public ResponseEntity<ApiResponse> getTodasRegistradas(@PathVariable Long idProyecto) {
        try {
            // Llamamos a la función que mezcla las vinculadas y las huérfanas
            List<GitLabTarea> todas = gitLabService.obtenerTodasDeBaseDatos(idProyecto);

            return ResponseEntity.ok(
                    new ApiResponse("Historial completo recuperado de Neon con éxito", true, todas));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al leer el historial: " + e.getMessage(), false, null));
        }
    }

    /**
     * Cambia la vinculación de una issue a otra tarea del proyecto.
     */
    @PutMapping("/vincular/{issueId}")
    public ResponseEntity<ApiResponse> modificarVinculacion(
            @PathVariable String issueId,
            @RequestParam Long nuevoIdTareaProyecto) {
        try {
            GitLabTarea modificada = gitLabService.modificarVinculacion(issueId, nuevoIdTareaProyecto);
            return ResponseEntity.ok(new ApiResponse("Vinculación modificada correctamente", true, modificada));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al modificar vinculación: " + e.getMessage(), false, null));
        }
    }

    /**
     * Desvincula y borra el registro local de la issue de GitLab.
     */
    @DeleteMapping("/vincular/{issueId}")
    public ResponseEntity<ApiResponse> eliminarVinculacion(@PathVariable String issueId) {
        try {
            gitLabService.eliminarVinculacion(issueId);
            return ResponseEntity.ok(new ApiResponse("Vinculación eliminada correctamente", true, null));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al eliminar vinculación: " + e.getMessage(), false, null));
        }
    }
}