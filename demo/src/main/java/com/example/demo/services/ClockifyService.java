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
import com.example.demo.dto.DetalleEstimacionDTO;
import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.GitLabTareaDTO;
import com.example.demo.dto.ProyectoClockifyDTO;
import com.example.demo.dto.SubFaseDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Excel;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.ExcelRepository;
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

        @Autowired
        private ExcelRepository excelRepository;

        @Value("${clockify.workspace.id}")
        private String workspaceId;

        ClockifyService(DetalleEstimacionService detalleEstimacionService) {
                this.detalleEstimacionService = detalleEstimacionService;
        }

        public List<ClockifyTareaDTO> obtenerTareasPorSubfase(Long projectId, String subfas) {

                String subfaseBuscada = detalleEstimacionService.normalizarTexto(subfas);

                Map<String, String> etiquetas = cargarMapaTagsClockify();

                Proyecto p = proyectoRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

                String clockifyId = p.getClockifyId();

                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // 🔹 DATOS NECESARIOS PARA VALIDACIÓN (igual que método inválidas)

                List<GitLabTareaDTO> tareasGit = gitLabService.obtenerTareasPorProyecto(projectId);

                Set<Long> ids = tareasGit.stream()
                                .map(GitLabTareaDTO::getIid)
                                .collect(Collectors.toSet());

                Excel exc = excelRepository.findFirstByIdProyectoAndVigenteTrue(projectId);

                List<DetalleEstimacionDTO> detalles = detalleEstimacionService
                                .obtenerDetallesPorExcel(exc.getIdExcel());

                Set<String> subValidas = detalles.stream()
                                .map(DetalleEstimacionDTO::getNombreSubfase)
                                .map(detalleEstimacionService::normalizarTexto)
                                .collect(Collectors.toSet());

                Set<String> tarValidas = detalles.stream()
                                .map(DetalleEstimacionDTO::getTarea)
                                .map(detalleEstimacionService::normalizarTexto)
                                .collect(Collectors.toSet());

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

                Map<String, ClockifyTareaDTO> tareasAgrupadas = new HashMap<>();

                for (Map<String, Object> entry : body) {

                        // 🔹 Filtrar por proyecto
                        if (!clockifyId.equals(entry.get("projectId")))
                                continue;

                        String description = (String) entry.get("description");
                        if (description == null || description.isEmpty())
                                continue;

                        // 🔹 1. Formato
                        boolean formatoCorrecto = description.matches("^\\[.+\\]#\\d+\\s.+$");

                        // 🔹 2. Subfase
                        String subfase = null;
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(
                                                description.indexOf("[") + 1,
                                                description.indexOf("]"));
                        }

                        String subfaseNormalizada = subfase != null
                                        ? detalleEstimacionService.normalizarTexto(subfase)
                                        : null;

                        // 🔹 3. ID Git
                        long idGit = -1;
                        if (description.contains("#")) {
                                try {
                                        int start = description.indexOf("#") + 1;
                                        int end = description.indexOf(" ", start);
                                        if (end == -1)
                                                end = description.length();

                                        idGit = Long.parseLong(description.substring(start, end));
                                } catch (Exception e) {
                                        // inválido
                                }
                        }

                        // 🔹 4. Título
                        String titulo = null;
                        if (description.contains("#")) {
                                int start = description.indexOf("#");
                                int firstSpace = description.indexOf(" ", start);

                                if (firstSpace != -1) {
                                        titulo = description.substring(firstSpace + 1).trim();
                                }
                        }

                        String tituloNormalizado = titulo != null
                                        ? detalleEstimacionService.normalizarTexto(titulo)
                                        : null;

                        // 🔹 5. Validaciones
                        boolean subfaseValida = subfaseNormalizada != null && subValidas.contains(subfaseNormalizada);
                        boolean tareaValida = tituloNormalizado != null && tarValidas.contains(tituloNormalizado);
                        boolean idValido = idGit != -1 && ids.contains(idGit);

                        boolean esValida = formatoCorrecto && subfaseValida && tareaValida && idValido;

                        // 🔴 Si NO es válida → fuera
                        if (!esValida)
                                continue;

                        // 🔹 Filtrar por subfase buscada
                        if (!subfaseNormalizada.equals(subfaseBuscada))
                                continue;

                        // 🔹 6. Tag
                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        String nombreTag = null;

                        if (tagIds != null && !tagIds.isEmpty()) {
                                nombreTag = etiquetas.get(tagIds.get(0));
                        }

                        // 🔹 7. Horas
                        Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                        String duration = (String) timeInterval.get("duration");
                        double horas = convertirDuracionAHoras(duration);

                        // 🔹 8. Agrupación
                        if (tareasAgrupadas.containsKey(description)) {

                                ClockifyTareaDTO dtoExistente = tareasAgrupadas.get(description);
                                dtoExistente.setHorasTrabajadas(dtoExistente.getHorasTrabajadas() + horas);

                        } else {

                                ClockifyTareaDTO nuevoDto = new ClockifyTareaDTO(
                                                titulo,
                                                horas,
                                                (int) idGit,
                                                nombreTag);

                                tareasAgrupadas.put(description, nuevoDto);
                        }
                }

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

                Map<String, String> etiquetas = cargarMapaTagsClockify();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                List<GitLabTareaDTO> tareasGit = gitLabService.obtenerTareasPorProyecto(projectId);

                ArrayList<Long> ids = new ArrayList<>();
                tareasGit.forEach(s -> {
                        ids.addLast(s.getIid());
                });

                Excel exc = excelRepository.findFirstByIdProyectoAndVigenteTrue(projectId);

                List<DetalleEstimacionDTO> detalles = detalleEstimacionService
                                .obtenerDetallesPorExcel(exc.getIdExcel());

                ArrayList<String> tareas = new ArrayList<>();
                detalles.forEach(s -> {
                        tareas.addLast(s.getTarea());
                });

                ArrayList<String> subfasesValidas = new ArrayList<>();
                detalles.forEach(s -> {
                        subfasesValidas.addLast(s.getNombreSubfase());
                });

                ArrayList<String> subValidas = new ArrayList<>();
                subfasesValidas.forEach(s -> {
                        subValidas.addLast(detalleEstimacionService.normalizarTexto(s));
                });
                ArrayList<String> tarValidas = new ArrayList<>();
                tareas.forEach(s -> {
                        tarValidas.addLast(detalleEstimacionService.normalizarTexto(s));
                });

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
                List<ClockifyTareaDTO> tareasInvalidas = new ArrayList<>();

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

                        List<String> tagIds = (List<String>) entry.get("tagIds");

                        String nombreTag = null;

                        if (tagIds != null && !tagIds.isEmpty()) {
                                String tagId = tagIds.get(0); // solo una etiqueta
                                nombreTag = etiquetas.get(tagId);
                        }

                        String titulo = null;

                        if (description.contains("#")) {
                                int start = description.indexOf("#");
                                int firstSpace = description.indexOf(" ", start);

                                if (firstSpace != -1) {
                                        titulo = description.substring(firstSpace + 1).trim();
                                }
                        }

                        String tituloNormalizado = titulo != null
                                        ? detalleEstimacionService.normalizarTexto(titulo)
                                        : null;

                        System.out.println(tituloNormalizado);

                        String subfaseNormalizada = subfase != null
                                        ? detalleEstimacionService.normalizarTexto(subfase)
                                        : null;

                        boolean tareaValida = tituloNormalizado != null
                                        && tarValidas.contains(tituloNormalizado);
                        boolean subfaseValida = subfaseNormalizada != null
                                        && subValidas.contains(subfaseNormalizada);

                        boolean idValido = idGit != -1 && ids.contains(idGit);

                        boolean esValida = formatoCorrecto && subfaseValida && idValido && tareaValida;

                        // 🟥 INVALIDA si:
                        // - formato incorrecto
                        // - o subfase no existe en BD
                        if (!esValida) {

                                Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                                String duration = (String) timeInterval.get("duration");
                                double horas = convertirDuracionAHoras(duration);

                                ClockifyTareaDTO dto = new ClockifyTareaDTO(description, horas, 0, nombreTag);
                                tareasInvalidas.add(dto);

                        } else {
                                ids.remove(idGit);
                        }
                }

                return tareasInvalidas;
        }

        public Map<String, String> cargarMapaTagsClockify() {

                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                String url = clockify.getUrlReal() + "/workspaces/" + workspaceId + "/tags";

                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                });

                List<Map<String, Object>> tags = response.getBody();

                Map<String, String> tagMap = new HashMap<>();

                if (tags != null) {
                        for (Map<String, Object> tag : tags) {
                                String id = (String) tag.get("id");
                                String name = (String) tag.get("name");
                                tagMap.put(id, name);
                        }
                }

                return tagMap;
        }

}