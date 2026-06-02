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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
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

        // 4. Petición paginada a la API externa (Evita el límite de las 20 tareas)
        List<Map<String, Object>> rawIssues = new ArrayList<>();
        int paginaActual = 1;
        boolean tieneMasPaginas = true;

        while (tieneMasPaginas) {
            // Construimos la URL inyectando los parámetros de paginación de GitLab
            String urlIssues = baseUrl + "/projects/" + gitlabId.trim() + "/issues?per_page=100&page=" + paginaActual;

            // Reutilizamos tu método para lanzar la consulta de la página actual
            List<Map<String, Object>> resultadoPagina = ejecutarConsultaLista(urlIssues, config.getClave());

            if (resultadoPagina != null && !resultadoPagina.isEmpty()) {
                rawIssues.addAll(resultadoPagina);
                paginaActual++; // Avanzamos a la siguiente página
            } else {
                tieneMasPaginas = false; // Paramos el bucle si la página viene vacía
            }
        }

        // 5. Transformación de modelo externo a DTO interno (Versión ultra-segura)
        return rawIssues.stream()
                .map(issue -> {
                    // 🛡️ CONTROL DE SEGURIDAD PARA EL ID GLOBAL
                    String idStr = issue.get("id") != null ? String.valueOf(issue.get("id")) : "0";

                    // 🛡️ CONTROL DE SEGURIDAD PARA EL NUMERO INTERNO (Evita el "For input string:
                    // null")
                    Object iidObj = issue.get("iid");
                    Long numeroGitLabVal = 0L; // Valor por defecto si viene roto o vacío
                    if (iidObj != null && !String.valueOf(iidObj).equals("null")
                            && !String.valueOf(iidObj).trim().isEmpty()) {
                        numeroGitLabVal = Long.valueOf(String.valueOf(iidObj).trim());
                    }

                    // 🛡️ CONTROL DE SEGURIDAD PARA TEXTOS
                    String titleStr = issue.get("title") != null ? String.valueOf(issue.get("title")) : "Sin título";
                    String stateStr = issue.get("state") != null ? String.valueOf(issue.get("state")) : "unknown";
                    List<String> labelsList = (List<String>) issue.get("labels");

                    // Construimos el DTO con los datos limpios y seguros
                    return new GitLabTareaDTO(
                            idStr,
                            numeroGitLabVal,
                            titleStr,
                            labelsList,
                            stateStr);
                })
                .collect(Collectors.toList());
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
     * [NUEVO] Recupera todas las tareas de GitLab y calcula su estado de
     * vinculación.
     * Si la tarea es nueva y tiene el nombre correcto, la guarda automáticamente en
     * Neon.
     * Si está mal escrita, la marca como no vinculada para que el front la
     * gestione.
     * * @param proyectoIdLocal ID del proyecto local.
     * 
     * @return Lista unificada de DTOs con el campo vinculada actualizado.
     */
    /**
     * Recupera todas las tareas de GitLab, calcula su estado de vinculación
     * y las persiste TODAS en Neon clasificándolas como válidas o inválidas.
     * * @param proyectoIdLocal ID del proyecto local.
     * 
     * @return Lista unificada de DTOs con el campo vinculada actualizado.
     */
    /**
     * Sincroniza las tareas desde GitLab y las persiste en Neon.
     * * @param proyectoIdLocal ID del proyecto local.
     * 
     * @return El número total de tareas NUEVAS que se han guardado en la base de
     *         datos.
     */
    public int sincronizarYContarNuevasTareas(Long proyectoIdLocal) {
        // 1. Descarga bruta desde GitLab
        List<GitLabTareaDTO> tareasGitLab = obtenerTareasPorProyecto(proyectoIdLocal);

        // 2. IDs ya persistidos en Neon para saber cuáles son viejas
        Set<String> idsEnBD = gitLabTareaRepository.findByIdProyecto(proyectoIdLocal).stream()
                .map(GitLabTarea::getIssueId)
                .collect(Collectors.toSet());
        // Contador para las tareas que guardemos nuevas
        int tareasNuevasGuardadas = 0;

        // 3. Precalculamos las colas de tareas locales en memoria por título
        Map<String, Queue<TareaProyecto>> colasPorTitulo = new HashMap<>();

        for (GitLabTareaDTO tarea : tareasGitLab) {
            if (idsEnBD.contains(tarea.getId()))
                continue;

            String titulo = (tarea.getTitle() != null) ? tarea.getTitle().trim() : "";
            if (titulo.isEmpty())
                continue;

            if (!colasPorTitulo.containsKey(titulo)) {
                List<TareaProyecto> coincidencias = tareaProyectoRepository.findByTarea(titulo);
                colasPorTitulo.put(titulo, new LinkedList<>(coincidencias));
            }
        }

        // 4. Procesamos la lista y guardamos SOLO las que no existían
        for (GitLabTareaDTO tarea : tareasGitLab) {

            if (idsEnBD.contains(tarea.getId())) {
                continue;
            }

            String titulo = (tarea.getTitle() != null) ? tarea.getTitle().trim() : "";

            GitLabTarea nueva = new GitLabTarea();
            nueva.setIssueId(tarea.getId());
            nueva.setNumeroGitlab(tarea.getNumeroGitLab());
            nueva.setTitulo(tarea.getTitle());
            nueva.setEstado(tarea.getEstado());

            Queue<TareaProyecto> cola = titulo.isEmpty() ? null : colasPorTitulo.get(titulo);

            if (cola != null && !cola.isEmpty()) {
                TareaProyecto tareaLocal = cola.poll();
                nueva.setTareaProyecto(tareaLocal.getIdTareaProyecto());
                nueva.setValida(true);
            } else {
                nueva.setTareaProyecto(null);
                nueva.setValida(false);
            }
            nueva.setIdProyecto(proyectoIdLocal);

            if (tarea.getLabels() != null && !tarea.getLabels().isEmpty()) {
                nueva.setDepartamento(tarea.getLabels().get(0));
            } else {
                nueva.setDepartamento("Sin Departamento");
            }

            System.out.println("DEBUG id a guardar: " + proyectoIdLocal);
            System.out.println("DEBUG valor en entidad: " + nueva.getIdProyecto());

            gitLabTareaRepository.save(nueva);
            tareasNuevasGuardadas++;
        }

        // 5. Devolvemos el número total de inserciones
        return tareasNuevasGuardadas;
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
    // Lógica para interactuar con la nueva tabla de Neon
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
    public GitLabTarea vincularTareaAProyecto(GitLabTareaDTO dto, Long idTareaProyecto) {

        // 1. Validar la existencia de la tarea de destino en la planificación local
        TareaProyecto tareaProy = tareaProyectoRepository.findById(idTareaProyecto)
                .orElseThrow(() -> new RuntimeException(
                        "La tarea de proyecto con ID " + idTareaProyecto + " no existe en el sistema."));

        // 2. Recuperar registro existente por ID global de GitLab para aplicar Upsert,
        // o instanciar uno nuevo
        GitLabTarea tarea = gitLabTareaRepository.findByIssueId(dto.getId())
                .orElse(new GitLabTarea());

        liberarVinculacionesPrevias(idTareaProyecto, tarea.getIssueId());

        // 3. Sincronizar estado y mapear el grafo de dependencias
        tarea.setIssueId(dto.getId());
        tarea.setNumeroGitlab(dto.getNumeroGitLab());
        tarea.setTitulo(dto.getTitle());
        tarea.setEstado(dto.getEstado());
        tarea.setTareaProyecto(idTareaProyecto); // Establece la relación de clave foránea (FK)
        tarea.setIdProyecto(tareaProy.getIdProyecto());
        // proyectoRepository.findById(tareaProy.getIdProyecto()).ifPresent(tarea::setIdProyecto);

        tarea.setValida(true);

        // 4. Persistir los cambios en el motor de base de datos relacional
        return gitLabTareaRepository.save(tarea);
    }

    /**
     * Recupera de Neon únicamente las tareas que tienen coincidencia exacta
     * con la planificación y cuyo estado es marcado como válido.
     * * @param idProyecto ID del proyecto local para filtrar.
     * 
     * @return Lista de entidades GitLabTarea (con @JsonIgnore aplicado).
     */
    public List<GitLabTarea> obtenerSoloValidasDeBaseDatos(Long idProyecto) {
        return gitLabTareaRepository.findValidasByProyectoIncluyendoVinculacion(idProyecto);
    }

    /**
     * Recupera de Neon el 100% de las tareas registradas de ese proyecto,
     * incluyendo las huérfanas/inválidas para que el usuario las gestione.
     * * @param idProyecto ID del proyecto local para filtrar.
     * 
     * @return Lista completa de control con todas las tareas asociadas.
     */
    public List<GitLabTarea> obtenerTodasDeBaseDatos(Long idProyecto) {
        return gitLabTareaRepository.findTodasByProyectoIncluyendoVinculacion(idProyecto);
    }

    public List<GitLabTarea> obtenerTareasPorDepartamento(Long idProyecto, String departamento) {
        if (departamento == null || departamento.trim().isEmpty()) {
            return new ArrayList<>(); // Devuelve vacío directamente si el parámetro llega mal
        }
        return gitLabTareaRepository.findByIdProyectoAndDepartamento(idProyecto, departamento.trim());
    }

    /**
     * Modifica una vinculación existente asociándola a un nuevo
     * idDetalleEstimacion.
     *
     * @param idGitlab                 ID único global de la issue en GitLab.
     * @param nuevoIdDetalleEstimacion El nuevo ID del Excel/Clockify.
     * @return La entidad GitLabTarea modificada.
     */
    public GitLabTarea modificarVinculacion(String issueId, Long nuevoIdTareaProyecto) {
        GitLabTarea tareaOrigen = gitLabTareaRepository.findByIssueId(issueId)
                .orElseThrow(() -> new RuntimeException("La tarea de GitLab con issue_id " + issueId + " no está registrada."));
        TareaProyecto nuevaTareaProyecto = tareaProyectoRepository.findById(nuevoIdTareaProyecto)
                .orElseThrow(() -> new RuntimeException("El nuevo id_tarea_proyecto " + nuevoIdTareaProyecto + " no existe."));
        // Guarda el id de la tarea local de la que venías (puede ser null si venía huérfana)
        Long idTareaVieja = tareaOrigen.getTareaProyecto();
        // Busca si ya hay alguna issue de GitLab ocupando el nuevo hueco de destino
        List<GitLabTarea> issuesEnDestino = gitLabTareaRepository.findByTareaProyectoAndValidaTrue(nuevoIdTareaProyecto);
        //  Lógica de Intercambio (Swap)
        if (issuesEnDestino != null && !issuesEnDestino.isEmpty()) {
            for (GitLabTarea issueDestino : issuesEnDestino) {
                // Evita hacernos swap a nosotros mismos si el front manda un ID repetido
                if (!issueDestino.getIssueId().equals(issueId)) {
                    if (idTareaVieja != null) {
                        // Si veníamos de otro sitio, mandamos a la issue estorbando a ese hueco libre
                        issueDestino.setTareaProyecto(idTareaVieja);
                        issueDestino.setValida(true);
                    } else {
                        // Si nosotros veníamos huérfanos, la issue estorbando se queda huérfana
                        issueDestino.setTareaProyecto(null);
                        issueDestino.setValida(false);
                    }
                    gitLabTareaRepository.save(issueDestino);
                }
            }
        }
        // Finalmente, asignamos la nueva tarea a la nuestra (Origen) en todos sus campos correspondientes
        tareaOrigen.setTareaProyecto(nuevaTareaProyecto.getIdTareaProyecto());
        tareaOrigen.setIdProyecto(nuevaTareaProyecto.getIdProyecto());
        tareaOrigen.setValida(true); // Se fuerza a true al ser una edición manual correcta
        return gitLabTareaRepository.save(tareaOrigen);
    }

    /**
     * Elimina de la base de datos la relación de una tarea de GitLab
     * (Desvincular).
     *
     * @param idGitlab ID único global de la issue a eliminar.
     */
    // Mantiene una sola issue valida por tarea local.
    private void liberarVinculacionesPrevias(Long idTareaProyecto, String issueIdActual) {
        if (idTareaProyecto == null) {
            return;
        }

        List<GitLabTarea> vinculacionesPrevias = gitLabTareaRepository
                .findByTareaProyectoAndValidaTrue(idTareaProyecto);

        List<GitLabTarea> vinculacionesALiberar = vinculacionesPrevias.stream()
                .filter(vinculacion -> !Objects.equals(vinculacion.getIssueId(), issueIdActual))
                .collect(Collectors.toList());

        if (vinculacionesALiberar.isEmpty()) {
            return;
        }

        vinculacionesALiberar.forEach(vinculacion -> {
            vinculacion.setValida(false);
            vinculacion.setTareaProyecto(null);
        });
        gitLabTareaRepository.saveAll(vinculacionesALiberar);
    }

    public GitLabTarea actualizarTituloIssue(String issueId, String titulo) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new RuntimeException("El titulo de la issue no puede estar vacio.");
        }

        GitLabTarea tarea = gitLabTareaRepository.findByIssueId(issueId)
                .orElseThrow(
                        () -> new RuntimeException("No se encontro ninguna issue registrada con ID: " + issueId));

        tarea.setTitulo(titulo.trim());
        return gitLabTareaRepository.save(tarea);
    }

    /**
     * Elimina de la base de datos la relación de una tarea de GitLab
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

    /**
     * Anula la relación entre una issue de GitLab y su tarea de proyecto actual.
     * Cambia el estado de la issue a huérfana (valida = false y tareaProyecto = null) 
     * sin borrar su registro de la base de datos.
     *
     * @param issueId ID único global de la issue de GitLab a desvincular.
     * @return La entidad GitLabTarea actualizada y guardada.
     * @throws RuntimeException Si la issue no se encuentra registrada en el sistema.
     */
    public GitLabTarea desvincularTarea(String issueId) {
        GitLabTarea tarea = gitLabTareaRepository.findByIssueId(issueId)
                .orElseThrow(() -> new RuntimeException("No se encontró ninguna issue registrada con ID: " + issueId));
        tarea.setTareaProyecto(null);
        tarea.setValida(false);
        return gitLabTareaRepository.save(tarea);
    }

}
