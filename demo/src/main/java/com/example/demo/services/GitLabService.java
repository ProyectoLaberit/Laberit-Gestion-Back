package com.example.demo.services;

import com.example.demo.dto.GitLabProyectoDTO;
import com.example.demo.dto.GitLabTareaDTO; // [NUEVO] Importamos el DTO
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
            
            String baseUrl = config.getUrlReal().endsWith("/")
                    ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                    : config.getUrlReal();

            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {};

            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", config.getClave());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    baseUrl + "/projects?owned=true",
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
     * [ACTUALIZADO] Ahora devuelve una lista de GitLabTareaDTO.
     * Incluye logs de depuración para la consola.
     */
    public List<GitLabTareaDTO> obtenerTareasPorProyecto(Long proyectoIdLocal) {
        // 1. Buscamos el proyecto
        Proyecto proyecto = proyectoRepository.findById(proyectoIdLocal)
                .orElseThrow(() -> new RuntimeException("Proyecto ID " + proyectoIdLocal + " no encontrado"));

        // 2. Debug: Verificamos qué ID de GitLab tiene en la base de datos
        System.out.println("DEBUG: Buscando tareas para Proyecto Local ID: " + proyectoIdLocal);
        System.out.println("DEBUG: ID GitLab asociado en DB: [" + proyecto.getGitlabId() + "]");

        if (proyecto.getGitlabId() == null || proyecto.getGitlabId().trim().isEmpty()) {
            System.out.println("DEBUG: El proyecto no tiene ID de GitLab. Abortando.");
            return List.of();
        }

        // 3. Configuración de API
        ApiConfig config = apiRepository.findByNombre("GitLab Maestro");
        String baseUrl = config.getUrlReal().endsWith("/")
                ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                : config.getUrlReal();

        // 4. Construcción de URL de Issues
        String urlIssues = baseUrl + "/projects/" + proyecto.getGitlabId().trim() + "/issues";
        System.out.println("DEBUG: Consultando URL: " + urlIssues);

        // 5. Obtenemos los mapas crudos y los transformamos al DTO limpio
        List<Map<String, Object>> rawIssues = ejecutarConsultaLista(urlIssues, config.getClave());
        
        System.out.println("DEBUG: Tareas recibidas de GitLab: " + rawIssues.size());

        return rawIssues.stream()
                .map(issue -> new GitLabTareaDTO(
                        issue.get("id"),
                        issue.get("iid"),
                        issue.get("title")
                ))
                .collect(Collectors.toList());
    }

    /**
     * Método genérico para peticiones GET.
     */
    private List<Map<String, Object>> ejecutarConsultaLista(String url, String token) {
        try {
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {};

            HttpHeaders headers = new HttpHeaders();
            headers.set("PRIVATE-TOKEN", token);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    tipoRespuesta);

            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("ERROR GITLAB API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            System.err.println("ERROR SISTEMA: " + e.getMessage());
            return List.of();
        }
    }

    public List<GitLabProyectoDTO> obtenerProyectosGitLabNoRegistrados() {
        List<String> idsYaGuardados = proyectoRepository.findAll()
                .stream()
                .map(p -> p.getGitlabId() != null ? p.getGitlabId().trim() : "")
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toList());

        List<Map<String, Object>> proyectosGitLab = obtenerProyectosDeGitLab();

        return proyectosGitLab.stream()
                .filter(proy -> {
                    Object idObj = proy.get("id");
                    if (idObj == null) return false;
                    String idGitLabStr = String.valueOf(idObj).trim();
                    return !idsYaGuardados.contains(idGitLabStr);
                })
                .map(proy -> new GitLabProyectoDTO(
                        String.valueOf(proy.get("id")).trim(),
                        String.valueOf(proy.get("name")).trim()))
                .collect(Collectors.toList());
    }
}