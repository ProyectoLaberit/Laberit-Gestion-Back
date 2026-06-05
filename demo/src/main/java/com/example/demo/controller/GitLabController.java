package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.GitLabProyectoDTO;
import com.example.demo.dto.GitLabTareaDTO;
import com.example.demo.entity.GitLabTarea;
import com.example.demo.services.GitLabService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
                    .ok(new ApiResponse("Tareas recuperadas de GitLab: " + tareasNuevas, true, tareasNuevas));
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

    /**
     * Endpoint para vincular una issue de GitLab con una tarea del proyecto local.
     *
     * @param tareaDTO        Datos de la issue que vienen desde GitLab.
     * @param idTareaProyecto ID de la tarea local de la base de datos
     *                        (Obligatorio).
     * @param urlProyecto     Enlace opcional al proyecto de GitLab.
     * @return ApiResponse con el resultado de la operación y un estado HTTP 200 o
     *         500.
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
     * Endpoint para obtener únicamente las tareas de GitLab que ya han sido
     * validadas.
     * Convierte las entidades de la base de datos a DTOs antes de enviarlas al
     * frontend.
     *
     * @param idProyecto ID del proyecto para acotar la búsqueda de tareas.
     * @return ApiResponse con la lista de tareas transformadas a DTO y estado HTTP
     *         200, o estado 500 si falla.
     */
    @GetMapping("/vinculadas/validas/{idProyecto}")
    public ResponseEntity<ApiResponse> getSoloValidadas(@PathVariable Long idProyecto) {
        try {
            // Recupera las entidades mapeadas desde la base de datos de Neon
            List<GitLabTarea> validas = gitLabService.obtenerSoloValidasDeBaseDatos(idProyecto);

            // Transforma la lista de entidades a una lista de DTOs para el Frontend
            // (Flutter)
            List<GitLabTareaDTO> dtos = validas.stream().map(GitLabTareaDTO::new).toList();

            return ResponseEntity.ok(new ApiResponse("Tareas válidas recuperadas de Neon con éxito", true, dtos));
        } catch (Exception e) {
            // Captura cualquier error de lectura en la base de datos
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al leer válidas: " + e.getMessage(), false, null));
        }
    }

    /**
     * Endpoint para obtener el listado completo de tareas registradas en el
     * proyecto.
     * Incluye tanto las tareas validadas como las inválidas o huérfanas,
     * convirtiéndolas a DTOs.
     *
     * @param idProyecto ID del proyecto para recuperar todo su historial de tareas.
     * @return ApiResponse con la lista completa de tareas en formato DTO y estado
     *         HTTP 200, o estado 500 si falla.
     */
    @GetMapping("/vinculadas/todas/{idProyecto}")
    public ResponseEntity<ApiResponse> getTodasRegistradas(@PathVariable Long idProyecto) {
        try {
            // Recupera todas las entidades almacenadas en la base de datos de Neon
            List<GitLabTarea> todas = gitLabService.obtenerTodasDeBaseDatos(idProyecto);

            // Transforma la lista de entidades a DTOs para el Frontend (Flutter)
            List<GitLabTareaDTO> dtos = todas.stream().map(GitLabTareaDTO::new).toList();

            return ResponseEntity.ok(new ApiResponse("Historial completo recuperado de Neon con éxito", true, dtos));
        } catch (Exception e) {
            // Captura cualquier error de lectura en la base de datos
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al leer el historial: " + e.getMessage(), false, null));
        }
    }

    /**
     * Endpoint para obtener las tareas de GitLab asociadas a un proyecto y
     * filtradas por un departamento específico.
     * Utiliza el identificador numérico del departamento y convierte los resultados
     * a DTOs.
     *
     * @param idProyecto     ID del proyecto para acotar el ámbito de búsqueda.
     * @param idDepartamento ID numérico del departamento por el que se desea
     *                       filtrar.
     * @return ApiResponse con la lista de tareas filtradas en formato DTO y estado
     *         HTTP 200, o estado 500 si falla.
     */
    @GetMapping("/vinculadas/departamento/{idProyecto}/{idDepartamento}")
    public ResponseEntity<ApiResponse> obtenerTareasPorDepartamento(
            @PathVariable Long idProyecto,
            @PathVariable Integer idDepartamento) {
        try {
            // Recupera de la base de datos las entidades que coinciden con el proyecto y el
            // departamento
            List<GitLabTarea> tareas = gitLabService.obtenerTareasPorDepartamento(idProyecto, idDepartamento);

            // Transforma la lista de entidades a DTOs para el Frontend (Flutter)
            List<GitLabTareaDTO> dtos = tareas.stream().map(GitLabTareaDTO::new).toList();

            return ResponseEntity.ok(new ApiResponse("Tareas por departamento recuperadas", true, dtos));
        } catch (Exception e) {
            // Captura cualquier error de lectura o de base de datos
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error: " + e.getMessage(), false, null));
        }
    }

    /**
     * Endpoint para modificar la vinculación existente de una issue de GitLab hacia
     * una nueva tarea local.
     * Actualiza la relación en la base de datos utilizando el identificador de la
     * issue.
     *
     * @param issueId              Identificador único de la issue de GitLab que se
     *                             va a reasignar.
     * @param nuevoIdTareaProyecto ID de la nueva tarea local a la que se asociará
     *                             la issue.
     * @return ApiResponse con la entidad modificada y estado HTTP 200, o estado 500
     *         si falla.
     */
    @PutMapping("/vincular/{issueId}")
    public ResponseEntity<ApiResponse> modificarVinculacion(
            @PathVariable String issueId,
            @RequestParam Long nuevoIdTareaProyecto) {
        try {
            // Invoca la lógica de negocio para actualizar la relación en la base de datos
            GitLabTarea modificada = gitLabService.modificarVinculacion(issueId, nuevoIdTareaProyecto);

            return ResponseEntity.ok(new ApiResponse("Vinculación modificada correctamente", true, modificada));
        } catch (Exception e) {
            // Captura cualquier error de actualización o restricciones de integridad
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al modificar vinculación: " + e.getMessage(), false, null));
        }
    }

    /**
     * Endpoint para actualizar el título de una issue de GitLab registrada en la
     * base de datos local.
     * Extrae el nuevo título directamente desde el cuerpo de la petición.
     *
     * @param issueId Identificador único de la issue de GitLab.
     * @param body    Mapa que contiene el nuevo título de la issue bajo la clave
     *                "titulo".
     * @return ApiResponse con la entidad actualizada y estado HTTP 200, o estado
     *         500 si falla.
     */
    @PutMapping("/issue/{issueId}/titulo")
    public ResponseEntity<ApiResponse> actualizarTituloIssue(
            @PathVariable String issueId,
            @RequestBody Map<String, String> body) {
        try {
            // Extrae de forma segura el campo título del cuerpo JSON
            String titulo = body != null ? body.get("titulo") : null;

            // Invoca la lógica de negocio para actualizar el registro en Neon
            GitLabTarea actualizada = gitLabService.actualizarTituloIssue(issueId, titulo);

            return ResponseEntity.ok(new ApiResponse("Titulo de issue actualizado correctamente", true, actualizada));
        } catch (Exception e) {
            // Captura cualquier fallo durante el proceso de actualización
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al actualizar titulo: " + e.getMessage(), false, null));
        }
    }

    /**
     * Endpoint para eliminar definitivamente el registro local de una issue de
     * GitLab.
     * Al borrarla, la issue podrá volver a descargarse en la próxima sincronización
     * si aún existe en GitLab.
     *
     * @param issueId Identificador único de la issue de GitLab que se desea
     *                eliminar.
     * @return ApiResponse confirmando la eliminación y un estado HTTP 200, o estado
     *         500 si falla.
     */
    @DeleteMapping("/borrar/{issueId}")
    public ResponseEntity<ApiResponse> eliminarVinculacion(@PathVariable String issueId) {
        try {
            // Invoca la lógica de negocio para borrar el registro de la base de datos de
            // Neon
            gitLabService.eliminarVinculacion(issueId);

            return ResponseEntity.ok(new ApiResponse("Vinculación eliminada correctamente", true, null));
        } catch (Exception e) {
            // Captura cualquier fallo durante el proceso de eliminación
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al eliminar vinculación: " + e.getMessage(), false, null));
        }
    }

    /**
     * Desvincula una issue de GitLab de su tarea de proyecto actual sin borrarla de
     * la base de datos.
     * La issue quedará como "inválida" o huérfana, estando disponible para ser
     * vinculada
     * a otra tarea estructural más adelante.
     *
     * @param issueId ID único global de la issue en GitLab a desvincular.
     * @return ResponseEntity con ApiResponse conteniendo la tarea desvinculada.
     */
    @PutMapping("/desvincular/{issueId}")
    public ResponseEntity<ApiResponse> desvincularTarea(@PathVariable String issueId) {
        try {
            GitLabTarea desvinculada = gitLabService.desvincularTarea(issueId);
            return ResponseEntity.ok(new ApiResponse("Tarea desvinculada correctamente", true, desvinculada));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al desvincular tarea: " + e.getMessage(), false, null));
        }
    }

    /**
     * Endpoint para obtener el listado de tareas de GitLab que aún no han sido
     * vinculadas a ninguna tarea local (huérfanas).
     * Recupera los registros inválidos de la base de datos y los transforma a DTOs.
     *
     * @param idProyecto ID del proyecto para filtrar las tareas huérfanas.
     * @return ApiResponse con la lista de tareas huérfanas en formato DTO y estado
     *         HTTP 200, o estado 500 si falla.
     */
    @GetMapping("/vinculadas/invalidas/{idProyecto}")
    public ResponseEntity<ApiResponse> getSoloInvalidas(@PathVariable Long idProyecto) {
        try {
            // Recupera de la base de datos las entidades que no tienen vinculación activa
            List<GitLabTarea> invalidas = gitLabService.obtenerSoloInvalidasDeBaseDatos(idProyecto);

            // Transforma la lista de entidades a DTOs para el Frontend (Flutter)
            List<GitLabTareaDTO> dtos = invalidas.stream().map(GitLabTareaDTO::new).toList();

            return ResponseEntity.ok(new ApiResponse("Tareas inválidas recuperadas de Neon con éxito", true, dtos));
        } catch (Exception e) {
            // Captura cualquier error durante la lectura en la base de datos
            return ResponseEntity.status(500)
                    .body(new ApiResponse("Error al leer inválidas: " + e.getMessage(), false, null));
        }
    }
}
