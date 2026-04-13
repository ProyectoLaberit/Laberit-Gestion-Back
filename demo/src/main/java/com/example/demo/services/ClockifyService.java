package com.example.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.ClockifyTareaDTO;
import com.example.demo.dto.ProyectoDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.ProyectoRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClockifyService {

        @Autowired
        ApiConfigRepository repositorioApi;

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        private ProyectoRepository proyectoRepository;

        @Value("${clockify.workspace.id}")
        private String workspaceId;

        public List<ClockifyTareaDTO> obtenerTareasPorSubfase(Long projectId, String subfaseBuscada) {

                Proyecto p = proyectoRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));
                String clockifyId = p.getClockifyId();

                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // 🔹 1. Obtener userId
                ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                                clockify.getUrlReal() + "/user",
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                String userId = (String) userResponse.getBody().get("id");

                // 🔹 2. Obtener time entries
                String url = clockify.getUrlReal() + "/workspaces/"
                                + workspaceId + "/user/" + userId + "/time-entries";
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                });
                List<Map<String, Object>> body = response.getBody();

                // 🟢 CAMBIO CLAVE: Usamos un Map para agrupar por la descripción de la tarea
                Map<String, ClockifyTareaDTO> tareasAgrupadas = new HashMap<>();

                for (Map<String, Object> entry : body) {

                        // 🔹 Filtrar por proyecto
                        if (!clockifyId.equals(entry.get("projectId")))
                                continue;

                        String description = (String) entry.get("description");
                        if (description == null)
                                continue;

                        // 🔹 1. Obtener subfase [backend]
                        String subfase = null;
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(
                                                description.indexOf("[") + 1,
                                                description.indexOf("]"));
                        }

                        // 🔹 Filtrar por subfase enviada desde front
                        if (subfase == null || !subfase.equalsIgnoreCase(subfaseBuscada))
                                continue;

                        // 🔹 2. Obtener idGit (#5)
                        int idGit = 0;
                        if (description.contains("#")) {
                                int start = description.indexOf("#") + 1;
                                int end = description.indexOf(" ", start);
                                if (end == -1)
                                        end = description.length();
                                idGit = Integer.parseInt(description.substring(start, end));
                        }

                        // 🔹 3. Obtener título (texto después del número)
                        String titulo = "";
                        if (description.contains("#")) {
                                int start = description.indexOf("#");
                                int firstSpace = description.indexOf(" ", start);
                                if (firstSpace != -1) {
                                        titulo = description.substring(firstSpace + 1).trim();
                                }
                        }

                        // 🔹 4. Obtener duración en horas
                        Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                        String duration = (String) timeInterval.get("duration"); // ej: PT2S

                        double horas = convertirDuracionAHoras(duration);

                        // 🟢 5. AGRUPACIÓN (La magia de la suma)
                        if (tareasAgrupadas.containsKey(description)) {
                                // Si la tarea ya existe en el mapa, recuperamos el DTO y le sumamos las horas
                                ClockifyTareaDTO dtoExistente = tareasAgrupadas.get(description);
                                double horasAcumuladas = dtoExistente.getHorasTrabajadas() + horas;
                                dtoExistente.setHorasTrabajadas(horasAcumuladas);
                        } else {
                                // Si es la primera vez que vemos esta tarea, la creamos y la metemos al mapa
                                ClockifyTareaDTO nuevoDto = new ClockifyTareaDTO(titulo, horas, idGit);
                                tareasAgrupadas.put(description, nuevoDto);
                        }
                }

                // 🟢 Devolvemos solo los valores del mapa transformados de nuevo en una Lista
                return new ArrayList<>(tareasAgrupadas.values());
        }

        private double convertirDuracionAHoras(String duration) {
                Duration d = Duration.parse(duration);
                long segundos = d.getSeconds();
                return segundos / 3600.0; // horas en decimal
        }
}