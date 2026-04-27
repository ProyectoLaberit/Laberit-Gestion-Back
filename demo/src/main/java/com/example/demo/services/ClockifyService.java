package com.example.demo.services;

import org.apache.tomcat.util.digester.SystemPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.demo.dto.ClockifyTareaDTO;
import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.GitLabTareaDTO;
import com.example.demo.dto.ProyectoClockifyDTO;
import com.example.demo.dto.SubFaseDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.ProyectoRepository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ClockifyService {

        private final DetalleEstimacionService detalleEstimacionService;

        @Autowired
        ApiConfigRepository repositorioApi;

        @Autowired
        private RestTemplate restTemplate;

        @Autowired
        private ProyectoRepository proyectoRepository;

        @Autowired
        private FaseService faseService;

        @Autowired
        private GitLabService gitLabService;

        @Value("${clockify.workspace.id}")
        private String workspaceId;

        ClockifyService(DetalleEstimacionService detalleEstimacionService) {
                this.detalleEstimacionService = detalleEstimacionService;
        }

        public List<ClockifyTareaDTO> obtenerTareasPorSubfase(Long projectId, String subfas) {

                String subfaseBuscada = detalleEstimacionService.normalizarTexto(subfas);

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

                        System.out.println(entry);

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

        public List<ProyectoClockifyDTO> obtenerProyectosNuevosDTO() {

                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                String url = clockify.getUrlReal() + "/workspaces/" + workspaceId + "/projects";

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());

                HttpEntity<String> entity = new HttpEntity<>(headers);

                // 🔹 1. Llamada a Clockify
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                });

                List<Map<String, Object>> proyectosClockify = response.getBody();

                // 🔹 2. Proyectos en BD
                List<Proyecto> proyectosBD = proyectoRepository.findAll();

                Set<String> idsBD = proyectosBD.stream()
                                .map(Proyecto::getClockifyId)
                                .collect(Collectors.toSet());

                // 🔹 3. Construir DTOs solo de los nuevos
                List<ProyectoClockifyDTO> resultado = new ArrayList<>();

                for (Map<String, Object> proyecto : proyectosClockify) {

                        String id = (String) proyecto.get("id");
                        String nombre = (String) proyecto.get("name");

                        if (!idsBD.contains(id)) {

                                ProyectoClockifyDTO dto = new ProyectoClockifyDTO();
                                dto.setId(id);
                                dto.setNombre(nombre);

                                resultado.add(dto);
                        }
                }

                return resultado;
        }

        // cando jorge me pase a lista de jorge cos numeros que cada vez que se encontre
        // un numero dos ids de gitlab se elimine da lista para que non haxa id
        // repetidos
        public List<ClockifyTareaDTO> obtenerEntradasInvalidas(Long projectId) {

                Proyecto p = proyectoRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

                String clockifyId = p.getClockifyId();
                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                List<GitLabTareaDTO> tareasGit = gitLabService.obtenerTareasPorProyecto(projectId);

                ArrayList<Long> ids = new ArrayList<>();
                tareasGit.forEach(s -> {
                        ids.addLast(s.getIid());
                });

                System.out.println(ids);

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

                // 🟢 3. Obtener subfases válidas del proyecto
                List<FaseDTO> fases = faseService.obtenerJerarquiaFasesPorProyecto(projectId);


                Set<String> subfasesValidas = fases.stream()
                                .flatMap(f -> f.getSubfases().stream())
                                .map(SubFaseDTO::getNombre)
                                .map(String::toLowerCase) // evitar problemas de mayúsculas
                                .collect(Collectors.toSet());

                List<ClockifyTareaDTO> tareasInvalidas = new ArrayList<>();

                ArrayList<String> validas = new ArrayList<>();
                subfasesValidas.forEach(s -> {
                        validas.addLast(detalleEstimacionService.normalizarTexto(s));
                });


                for (Map<String, Object> entry : body) {


                        // 🔹 Filtrar por proyecto
                        if (!clockifyId.equals(entry.get("projectId")))
                                continue;

                        String description = (String) entry.get("description");
                        if (description == null || description.isEmpty())
                                continue;

                        // 🔹 1. Validar formato completo
                        boolean formatoCorrecto = description.matches("^\\[.+\\]#\\d+\\s.+$");


                        String subfase = null;

                        // 🔹 2. Extraer subfase si existe
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(
                                                description.indexOf("[") + 1,
                                                description.indexOf("]"));
                        }

                        long idGit = -1;

                        if (description.contains("#")) {
                                try {
                                        int start = description.indexOf("#") + 1;
                                        int end = description.indexOf(" ", start);
                                        if (end == -1)
                                                end = description.length();

                                        idGit = Long.parseLong(description.substring(start, end));
                                } catch (Exception e) {
                                        // id mal formado → lo tratamos como inválido
                                }
                        }

                        boolean subfaseValida = subfase != null && validas.contains(subfase.toLowerCase()) && ids.contains(idGit);




                        // 🟥 INVALIDA si:
                        // - formato incorrecto
                        // - o subfase no existe en BD
                        if (!formatoCorrecto || !subfaseValida) {

                                
                                Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                                String duration = (String) timeInterval.get("duration");
                                double horas = convertirDuracionAHoras(duration);

                                ClockifyTareaDTO dto = new ClockifyTareaDTO(description, horas, 0);
                                tareasInvalidas.add(dto);
                        }else{
                                ids.remove(idGit);
                        }
                }

                return tareasInvalidas;
        }

}