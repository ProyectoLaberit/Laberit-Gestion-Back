package com.example.demo.services;

import com.example.demo.entity.ApiConfig;
import com.example.demo.repository.ApiConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;

@Service
public class GitLabService {

    @Autowired
    private ApiConfigRepository apiRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public List<Map<String, Object>> obtenerProyectosDeGitLab() {
        try {
            ApiConfig config = apiRepository.findByNombre("GitLab Maestro")
                .orElseThrow(() -> new RuntimeException("Configuración no encontrada"));

            String urlCompleta = config.getUrlReal() + "?owned=true&access_token=" + config.getClave();

            // Esta es la clave: Le decimos a Java exactamente qué estructura esperar
            ParameterizedTypeReference<List<Map<String, Object>>> tipoRespuesta = 
                new ParameterizedTypeReference<List<Map<String, Object>>>() {};

            // Hacemos la llamada pidiendo una LISTA directamente
            ResponseEntity<List<Map<String, Object>>> respuesta = restTemplate.exchange(
                urlCompleta, 
                HttpMethod.GET, 
                null, 
                tipoRespuesta
            );

            // Devolvemos el cuerpo de la respuesta (que ya es una List<Map>)
            return respuesta.getBody() != null ? respuesta.getBody() : List.of();

        } catch (Exception e) {
            System.err.println("Error al conectar con GitLab: " + e.getMessage());
            return List.of();
        }
    }
}