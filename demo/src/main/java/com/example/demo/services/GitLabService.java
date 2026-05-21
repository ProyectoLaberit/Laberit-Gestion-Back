package com.example.demo.services;

import com.example.demo.dto.GitLabProyectoDTO;
import com.example.demo.dto.GitLabTareaDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.Proyecto;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.GitLabTareaRepository;
import com.example.demo.repository.ProyectoRepository;
import com.example.demo.repository.TareaProyectoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GitLabService {

    @Autowired
    private ApiConfigRepository apiRepository;

    @Autowired
    private ProyectoRepository proyectoRepository;

    @Autowired
    private GitLabTareaRepository gitLabTareaRepository;

    @Autowired
    private TareaProyectoRepository tareaProyectoRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Consulta la API de GitLab para obtener los proyectos donde el Token Maestro
     * es propietario.
     * * Flujo de ejecución:
     * 1. Recupera la configuración de acceso (URL y Clave) desde la base de datos.
     * 2. Normaliza la URL base eliminando barras diagonales finales para evitar
     * errores de sintaxis.
     * 3. Configura el encabezado de seguridad 'PRIVATE-TOKEN'.
     * 4. Realiza la petición GET al endpoint /projects filtrando por propiedad
     * (owned=true).
     * * @return List de Mapas con la respuesta cruda de GitLab. Retorna una lista
     * vacía si ocurre un error.
     */
    public List<Map<String, Object>> obtenerProyectosDeGitLab() {
        try {
            // 1. Localización de credenciales maestras
            ApiConfig config = apiRepository.findByNombre("GitLab Maestro");

            // 2. Limpieza de URL para asegurar que el endpoint sea válido
            String baseUrl = config.getUrlReal().endsWith("/")
                    ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                    : config.getUrlReal();

            // Definimos que esperamos una lista de mapas (clave-valor)
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {
            };

            // 3. Preparación de la autenticación
            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", config.getClave());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // 4. Llamada externa a la API de GitLab
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    baseUrl + "/projects?owned=true",
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

            // Devolvemos el cuerpo o una lista vacía para evitar retornos nulos
            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (Exception e) {
            // Log de error simplificado para monitorización
            System.err.println("Error al conectar con GitLab: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Recupera y transforma las tareas de GitLab asociadas a un proyecto local.
     * * Flujo del proceso:
     * 1. Valida la existencia del proyecto en la base de datos local.
     * 2. Verifica que el proyecto tenga vinculado un ID externo de GitLab.
     * 3. Consulta el endpoint de 'issues' de la API v4 de GitLab.
     * 4. Mapea la respuesta cruda (Map) a una lista de objetos GitLabTareaDTO.
     *
     * @param proyectoIdLocal ID del proyecto en nuestra base de datos local.
     * @return Una lista de GitLabTareaDTO con los datos esenciales de la tarea.
     * @throws RuntimeException Si el proyecto no existe en el sistema local.
     */
    public List<GitLabTareaDTO> obtenerTareasPorProyecto(Long proyectoIdLocal) {
        // 1. Validación de persistencia local
        Proyecto proyecto = proyectoRepository.findById(proyectoIdLocal)
                .orElseThrow(() -> new RuntimeException("Proyecto ID " + proyectoIdLocal + " no encontrado"));

        // 2. Validación de vinculación con GitLab
        String gitlabId = proyecto.getGitlabId();
        if (gitlabId == null || gitlabId.trim().isEmpty()) {
            System.out.println("WARN: El proyecto local " + proyectoIdLocal + " no tiene un ID de GitLab vinculado.");
            return List.of();
        }

        // 3. Obtención de credenciales y normalización de endpoint
        ApiConfig config = apiRepository.findByNombre("GitLab Maestro");
        String baseUrl = config.getUrlReal().endsWith("/")
                ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                : config.getUrlReal();

        // 4. Petición a la API externa (Aquí es donde se produce la latencia de red)
        String urlIssues = baseUrl + "/projects/" + gitlabId.trim() + "/issues";
        List<Map<String, Object>> rawIssues = ejecutarConsultaLista(urlIssues, config.getClave());

        // 5. Transformación de modelo externo a DTO interno
        return rawIssues.stream()
                .map(issue -> new GitLabTareaDTO(
                        String.valueOf(issue.get("id")), // ID global de GitLab
                        Long.valueOf(String.valueOf(issue.get("numeroGitLab"))), // ID interno del proyecto
                        String.valueOf(issue.get("title")), // Título de la tarea
                        (List<String>) issue.get("labels"), // Etiquetas
                        String.valueOf(issue.get("state")) // <--- GitLab trae "state", tu DTO lo guarda como "estado"
                ))
                .collect(Collectors.toList());// Etiquetas asociadas a la tarea

    }

    /**
     * Ejecuta una petición HTTP GET genérica hacia la API de GitLab.
     * * Este método centraliza la lógica de comunicación para evitar duplicidad de
     * código.
     * Se encarga de gestionar las cabeceras de autenticación y de capturar errores
     * específicos de la conexión REST.
     *
     * @param url   URL completa del endpoint a consultar.
     * @param token Token de acceso privado para la autenticación.
     * @return Una lista de mapas con los datos de respuesta. Si falla, retorna una
     *         lista vacía.
     */
    private List<Map<String, Object>> ejecutarConsultaLista(String url, String token) {
        try {
            // Definición del tipo de dato esperado: Lista de estructuras clave-valor
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {
            };

            // Configuración de la cabecera de seguridad requerida por GitLab
            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Ejecución de la llamada mediante RestTemplate
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Error específico de la API (Ej: 404 Not Found o 401 Unauthorized)
            System.err.println("ERROR GITLAB API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            // Error genérico del sistema (Ej: fallo de red o timeout)
            System.err.println("ERROR SISTEMA: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Identifica y devuelve los proyectos de GitLab que aún no han sido registrados
     * en la base de datos local.
     * * El proceso realiza un filtrado por exclusión:
     * 1. Obtiene todos los IDs de GitLab ya almacenados en nuestra base de datos
     * local.
     * 2. Recupera la lista completa de proyectos desde la API externa de GitLab.
     * 3. Filtra la lista externa eliminando aquellos proyectos cuyo ID ya existe en
     * nuestra base de datos.
     * 4. Transforma los proyectos restantes en objetos GitLabProyectoDTO.
     * * @return List de GitLabProyectoDTO con los proyectos disponibles para ser
     * importados.
     */
    public List<GitLabProyectoDTO> obtenerProyectosGitLabNoRegistrados() {
        // 1. Recopilación de identificadores locales para la comparación
        List<String> idsYaGuardados = proyectoRepository.findAll()
                .stream()
                .map(p -> p.getGitlabId() != null ? p.getGitlabId().trim() : "")
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        // 2. Llamada al servicio que conecta con la API externa
        List<Map<String, Object>> proyectosGitLab = obtenerProyectosDeGitLab();

        // 3. Lógica de filtrado y mapeo a DTO
        return proyectosGitLab.stream()
                .filter(proy -> {
                    Object idObj = proy.get("id");
                    if (idObj == null)
                        return false;

                    String idGitLabStr = String.valueOf(idObj).trim();
                    // Solo incluimos el proyecto si su ID no está en nuestra lista local
                    return !idsYaGuardados.contains(idGitLabStr);
                })
                .map(proy -> new GitLabProyectoDTO(
                        String.valueOf(proy.get("id")).trim(),
                        String.valueOf(proy.get("name")).trim()))
                .collect(Collectors.toList());
    }

    // =========================================================================
    // [NUEVOS MÉTODOS] Lógica para interactuar con la nueva tabla de Neon
    // "tareas_gitlab"
    // =========================================================================

    /**
     * Vincula de forma persistente una issue de GitLab con una tarea de
     * planificación local.
     * <p>
     * Aplica una estrategia de "Upsert" (Update/Insert): si la issue ya se
     * encuentra
     * registrada localmente, actualiza sus metadatos y su asignación; en caso
     * contrario,
     * inicializa un nuevo registro.
     * </p>
     *
     * @param dto             Objeto de transferencia de datos con la información
     *                        origen de GitLab.
     * @param idTareaProyecto Identificador único de la tarea de proyecto destino en
     *                        el sistema local.
     * @param urlProyecto     Dirección web de acceso directo a la issue en la
     *                        plataforma externa.
     * @return La entidad {@link GitLabTarea} gestionada y persistida en el
     *         repositorio.
     * @throws RuntimeException Si el identificador de la tarea de proyecto no se
     *                          localiza en la base de datos.
     */
    public GitLabTarea vincularTareaAProyecto(GitLabTareaDTO dto, Long idTareaProyecto, String urlProyecto) {

        // 1. Validar la existencia de la tarea de destino en la planificación local
        TareaProyecto tareaProy = tareaProyectoRepository.findById(idTareaProyecto)
                .orElseThrow(() -> new RuntimeException(
                        "La tarea de proyecto con ID " + idTareaProyecto + " no existe en el sistema."));

        // 2. Recuperar registro existente por ID global de GitLab para aplicar Upsert,
        // o instanciar uno nuevo
        GitLabTarea tarea = gitLabTareaRepository.findByIssueId(dto.getId())
                .orElse(new GitLabTarea());

        // 3. Sincronizar estado y mapear el grafo de dependencias
        tarea.setIssueId(dto.getId());
        tarea.setnumeroGitLab(dto.getnumeroGitLab());
        tarea.setTitulo(dto.getTitle());
        tarea.setEstado(dto.getEstado());
        tarea.setUrl(urlProyecto);
        tarea.setTareaProyecto(tareaProy); // Establece la relación de clave foránea (FK)

        // 4. Persistir los cambios en el motor de base de datos relacional
        return gitLabTareaRepository.save(tarea);
    }

    /**
     * [NUEVO] Recupera todas las tareas que ya han sido registradas y vinculadas
     * localmente.
     * * @return Lista de entidades GitLabTarea.
     */
    public List<GitLabTarea> obtenerTareasVinculadasLocal() {
        return gitLabTareaRepository.findAll();
    }

    /**
     * [NUEVO] Modifica una vinculación existente asociándola a un nuevo
     * idDetalleEstimacion.
     *
     * @param idGitlab                 ID único global de la issue en GitLab.
     * @param nuevoIdDetalleEstimacion El nuevo ID del Excel/Clockify.
     * @return La entidad GitLabTarea modificada.
     */
    public GitLabTarea modificarVinculacion(String issueId, Long nuevoIdTareaProyecto) {
        GitLabTarea tarea = gitLabTareaRepository.findByIssueId(issueId)
                .orElseThrow(() -> new RuntimeException(
                        "La tarea de GitLab con issue_id " + issueId + " no está registrada."));

        TareaProyecto nuevaTareaProyecto = tareaProyectoRepository.findById(nuevoIdTareaProyecto)
                .orElseThrow(() -> new RuntimeException(
                        "El nuevo id_tarea_proyecto " + nuevoIdTareaProyecto + " no existe."));

        tarea.setTareaProyecto(nuevaTareaProyecto);
        return gitLabTareaRepository.save(tarea);
    }

    /**
     * [NUEVO] Elimina de la base de datos la relación de una tarea de GitLab
     * (Desvincular).
     *
     * @param idGitlab ID único global de la issue a eliminar.
     */
    public void eliminarVinculacion(String issueId) {
        GitLabTarea tarea = gitLabTareaRepository.findByIssueId(issueId)
                .orElseThrow(
                        () -> new RuntimeException("No se encontró ninguna vinculación para la issue: " + issueId));

        gitLabTareaRepository.delete(tarea);
    }

}