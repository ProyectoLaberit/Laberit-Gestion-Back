package com.example.demo.services;

import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.ProyectoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
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

            String urlCompleta = config.getUrlReal() + "?owned=true&access_token=" + config.getClave();

            // Esta es la clave: Le decimos a Java exactamente qué estructura esperar
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<List<Map<String, Object>>>() {
            };

            // Hacemos la llamada pidiendo una LISTA directamente
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    urlCompleta,
                    HttpMethod.GET,
                    null,
                    tipoRespuesta);

            // Devolvemos el cuerpo de la respuesta (que ya es una List<Map>)
            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (Exception e) {
            System.err.println("Error al conectar con GitLab: " + e.getMessage());
            return List.of();
        }
    }

    // --- MÉTODOS PARA INTEGRACIÓN CON GITLAB ---

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
        // Se concatena el gitlabId del proyecto y el token de acceso maestro
        String urlIssues = baseUrl + "/projects/" + proyecto.getGitlabId() + "/issues?access_token="
                + config.getClave();

        // 5. Ejecutamos la petición HTTP y devolvemos la lista de tareas
        return ejecutarConsultaLista(urlIssues);
    }

    /**
     * Consulta los proyectos en GitLab usando un token de usuario y devuelve solo
     * aquellos que aún NO están registrados en nuestra base de datos local.
     */
    public List<Map<String, Object>> filtrarProyectosExternos(String tokenGitlab) {
        // 1. Obtenemos la URL base configurada para GitLab
        ApiConfig config = apiRepository.findByNombre("GitLab Maestro");
        String baseUrl = config.getUrlReal().endsWith("/")
                ? config.getUrlReal().substring(0, config.getUrlReal().length() - 1)
                : config.getUrlReal();

        // 2. Construimos la URL para traer los proyectos donde el usuario es dueño
        // (owned=true)
        // Usamos el token que llega desde el frontend (específico del usuario actual)
        String urlGitLab = baseUrl + "/projects?owned=true&access_token=" + tokenGitlab;

        // 3. Obtenemos todos los proyectos disponibles en la cuenta de GitLab del
        // usuario
        List<Map<String, Object>> proyectosGitLab = ejecutarConsultaLista(urlGitLab);

        // 4. Obtenemos de nuestra DB todos los IDs de GitLab que ya tenemos guardados
        List<String> idsLocales = proyectoRepository.findAll().stream()
                .map(Proyecto::getGitlabId)
                .filter(id -> id != null) // Evitamos nulos para que no falle la comparación
                .collect(Collectors.toList());

        // 5. Filtramos la lista de GitLab: solo dejamos los que NO existan en
        // 'idsLocales'
        return proyectosGitLab.stream()
                .filter(p -> !idsLocales.contains(p.get("id").toString()))
                .collect(Collectors.toList());
    }

    /**
     * Método genérico privado para realizar peticiones GET a la API y
     * deserializar la respuesta en una lista de mapas (JSON).
     */
    private List<Map<String, Object>> ejecutarConsultaLista(String url) {
        try {
            // Definimos el tipo de retorno esperado: una Lista de Mapas (List<Map<String,
            // Object>>)
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = new ParameterizedTypeReference<>() {
            };

            // Realizamos la llamada HTTP mediante RestTemplate
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
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
}