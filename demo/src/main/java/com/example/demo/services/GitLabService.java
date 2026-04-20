package com.example.demo.services;

import com.example.demo.dto.GitLabProyectoDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.ProyectoRepository;
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

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> obtenerProyectosDeGitLab() {
        try {
            ApiConfig config = apiRepository.findByNombre("GitLab Maestro");
            // .orElseThrow(() -> new RuntimeException("Configuración no encontrada"));

            // Limpiamos la URL para asegurar que no termine en '/' y evitar errores al
            // concatenar
            String baseUrl = config.getUrlReal().endsWith("/")
                    ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                    : config.getUrlReal();

            // Esta es la clave: Le decimos a Java exactamente qué estructura esperar
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<List<Map<String, Object>>>() {
            };

            // Configuramos las cabeceras (headers) para incluir la clave de autenticación.
            // Esto permite que el servidor valide nuestra identidad de forma segura
            // sin exponer el token directamente en la URL.
            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", config.getClave());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Hacemos la llamada pidiendo una LISTA directamente
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    baseUrl + "/projects?owned=true",
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

            // Devolvemos el cuerpo de la respuesta (que ya es una List<Map>)
            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (Exception e) {
            System.err.println("Error al conectar con GitLab: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Obtiene la lista de incidencias (issues/tareas) de un proyecto específico en
     * GitLab.
     * Utiliza el ID local del proyecto para buscar su equivalente en GitLab.
     */
    public List<Map<String, Object>> obtenerTareasPorProyecto(Long proyectoIdLocal) {
        // 1. Buscamos el proyecto en nuestra base de datos local
        Proyecto proyecto = proyectoRepository.findById(proyectoIdLocal)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado en la base de datos"));

        // 2. Verificamos que el proyecto tenga vinculado un ID de GitLab (importante
        // para la API externa)
        if (proyecto.getGitlabId() == null || proyecto.getGitlabId().isEmpty()) {
            throw new RuntimeException("El proyecto no tiene un ID de GitLab asociado.");
        }

        // 3. Recuperamos la configuración maestra de la API (URL y Token Maestro) desde
        // la DB
        ApiConfig config = apiRepository.findByNombre("GitLab Maestro");

        // Limpiamos la URL para asegurar que no termine en '/' y evitar errores al
        // concatenar
        String baseUrl = config.getUrlReal().endsWith("/")
                ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                : config.getUrlReal();

        // 4. Construimos la URL específica para obtener los "issues" del proyecto en
        // GitLab
        // Se concatena el gitlabId del proyecto (sin exponer el token en la URL)
        String urlIssues = baseUrl + "/projects/" + proyecto.getGitlabId() + "/issues";

        // 5. Ejecutamos la petición HTTP y devolvemos la lista de tareas
        return ejecutarConsultaLista(urlIssues, config.getClave());
    }

    /**
     * Método genérico privado para realizar peticiones GET a la API y
     * deserializar la respuesta en una lista de mapas (JSON).
     */
    private List<Map<String, Object>> ejecutarConsultaLista(String url, String token) {
        try {
            // Definimos el tipo de retorno esperado: una Lista de Mapas (List<Map<String,
            // Object>>)
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {
            };

            // Configuramos las cabeceras con el token de autenticación
            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Realizamos la llamada HTTP mediante RestTemplate
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

            // Si el cuerpo de la respuesta es nulo, devolvemos una lista vacía para evitar
            // NullPointerException
            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (Exception e) {
            // En caso de error (401, 404, error de red), devolvemos lista vacía
            // Podrías añadir un log aquí para depuración:
            // System.out.println(e.getMessage());
            return List.of();
        }
    }

    /**
     * Devuelve los proyectos de GitLab que aún no están registrados en la base de
     * datos.
     */
    public List<GitLabProyectoDTO> obtenerProyectosGitLabNoRegistrados() {

        // 1. Forzamos que la lista sea de Strings limpios
        List<String> idsYaGuardados = proyectoRepository.findAll()
                .stream()
                .map(p -> p.getGitlabId() != null ? p.getGitlabId().trim() : "")
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        List<Map<String, Object>> proyectosGitLab = obtenerProyectosDeGitLab();

        return proyectosGitLab.stream()
                .filter(proy -> {
                    Object idObj = proy.get("id");
                    if (idObj == null)
                        return false;

                    // IMPORTANTE: GitLab devuelve IDs numéricos,
                    // String.valueOf asegura que se conviertan bien a texto para comparar
                    String idGitLabStr = String.valueOf(idObj).trim();

                    return !idsYaGuardados.contains(idGitLabStr);
                })
                .map(proy -> new GitLabProyectoDTO(
                        String.valueOf(proy.get("id")).trim(),
                        String.valueOf(proy.get("name")).trim()))
                .collect(Collectors.toList());
    }
}