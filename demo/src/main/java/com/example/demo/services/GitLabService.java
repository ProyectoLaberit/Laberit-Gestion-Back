package com.example.demo.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Arrays;
import java.util.Map;

@Service
public class GitLabService {

    private final String TOKEN = "glpat-inNrsKc9DN0J5Xxs_inK8m86MQp1OmwxdzV3Cw.01.1203a5ie0";
    
    private final String URL_GITLAB = "https://gitlab.com/api/v4/projects?owned=true&access_token=";

    // Lee el json con la información de los proyectos.
    private final RestTemplate restTemplate = new RestTemplate();


    public List<Map<String, Object>> obtenerProyectosDeGitLab() {
        
        // Juntamos la dirección con nuestra llave para poder pasar
        String urlCompleta = URL_GITLAB + TOKEN;

        try {
            // Pedimos a GitLab la lista de proyectos
            Map[] respuesta = restTemplate.getForObject(urlCompleta, Map[].class);

            // Si todo ha ido bien, transformamos esos datos en una Lista fácil de leer
            return Arrays.asList((Map<String, Object>[]) respuesta);
            
        } catch (Exception e) {
            // Si el token está mal o no hay internet, devolvemos una lista vacía para que no explote el programa
            System.err.println("Error al conectar con GitLab: " + e.getMessage());
            return List.of();
        }
    }
}
