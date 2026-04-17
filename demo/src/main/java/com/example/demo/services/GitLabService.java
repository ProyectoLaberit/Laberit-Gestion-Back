package com.example.demo.services;

import com.example.demo.dto.GitLabProyectoDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import org.springframework.http.HttpHeaders;
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

            String baseUrl = config.getUrlReal().endsWith("/")
                    ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                    : config.getUrlReal();

            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {
            };

            HttpEntity<String> entity = crearEntityConToken(config.getClave());

            // Ya no necesitas ?access_token= en la URL, el token va en el header
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    baseUrl + "/projects",
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

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
        Proyecto proyecto = proyectoRepository.findById(proyectoIdLocal)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado en la base de datos"));

        if (proyecto.getGitlabId() == null || proyecto.getGitlabId().isEmpty()) {
            throw new RuntimeException("El proyecto no tiene un ID de GitLab asociado.");
        }

        ApiConfig config = apiRepository.findByNombre("GitLab Maestro");

        String baseUrl = config.getUrlReal().endsWith("/")
                ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                : config.getUrlReal();

        // ✅ URL limpia, sin access_token
        String urlIssues = baseUrl + "/projects/" + proyecto.getGitlabId() + "/issues";

        // ✅ Se pasa la URL y el TOKEN por separado
        return ejecutarConsultaLista(urlIssues, config.getClave());
    }

    /**
     * Método genérico privado para realizar peticiones GET a la API y
     * deserializar la respuesta en una lista de mapas (JSON).
     */
    private List<Map<String, Object>> ejecutarConsultaLista(String url, String token) {
        try {
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {
            };

            HttpEntity<String> entity = crearEntityConToken(token);

            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (Exception e) {
            System.err.println("Error en consulta: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Devuelve los proyectos de GitLab que aún no están registrados en la base de
     * datos.
     */
    public List<GitLabProyectoDTO> obtenerProyectosGitLabNoRegistrados() {

        // 1. Obtenemos todos los gitlabId que ya tenemos guardados en la DB
        // Filtramos los nulos para evitar errores en la comparación posterior
        List<String> idsYaGuardados = proyectoRepository.findAll()
                .stream()
                .map(p -> p.getGitlabId())
                .filter(id -> id != null)
                .collect(Collectors.toList());

        // 2. Llamamos a GitLab usando el token "GitLab Maestro" de la DB
        // y obtenemos todos los proyectos de esa cuenta
        List<Map<String, Object>> proyectosGitLab = obtenerProyectosDeGitLab();

        // 3. Filtramos los proyectos de GitLab que NO están en nuestra DB
        // y los convertimos al DTO con solo id y nombre
        return proyectosGitLab.stream()
                .filter(proy -> {
                    Object idGitLab = proy.get("id");
                    return idGitLab != null && !idsYaGuardados.contains(idGitLab.toString());
                })
                .map(proy -> new GitLabProyectoDTO(
                        proy.get("id").toString(),
                        proy.get("name").toString()))
                .collect(Collectors.toList());
    }

    private HttpEntity<String> crearEntityConToken(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("PRIVATE-TOKEN", token);
        return new HttpEntity<>(headers);
    }
}