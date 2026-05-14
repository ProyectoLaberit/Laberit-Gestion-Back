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

import com.example.demo.annotation.Auditable;
import com.example.demo.dto.ClockifyTareaDTO;
import com.example.demo.dto.DetalleEstimacionDTO;
import com.example.demo.dto.FaseDTO;
import com.example.demo.dto.GitLabTareaDTO;
import com.example.demo.dto.ProyectoClockifyDTO;
import com.example.demo.dto.SubFaseDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Departamento;
import com.example.demo.entity.DetalleEstimacion;
import com.example.demo.entity.Excel;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.entity.Proyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.DetalleEstimacionRepository;
import com.example.demo.repository.ExcelRepository;
import com.example.demo.repository.FaseRepository;
import com.example.demo.repository.ProyectoRepository;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
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
        private FaseRepository faseRepository;

        @Autowired
        private GitLabService gitLabService;

        @Autowired
        private ExcelRepository excelRepository;

        @Autowired
        private ImputacionClockifyService imputacionService;

        @Autowired
        private DetalleEstimacionRepository detalleEstimacionRepository;

        @Value("${clockify.workspace.id}")
        private String workspaceId;

        @Autowired
        private DepartamentoRepository departamentoRepository;

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

                ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                                clockify.getUrlReal() + "/user",
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });

                String userId = (String) userResponse.getBody().get("id");

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

                        if (!clockifyId.equals(entry.get("projectId")))
                                continue;

                        String description = (String) entry.get("description");
                        if (description == null || description.isEmpty())
                                continue;

                        boolean formatoCorrecto = description.matches("^\\[.+\\]#\\d+\\s.+$");

                        String subfase = null;
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(
                                                description.indexOf("[") + 1,
                                                description.indexOf("]"));
                        }

                        String subfaseNormalizada = subfase != null
                                        ? detalleEstimacionService.normalizarTexto(subfase)
                                        : null;

                        long idGit = -1;
                        if (description.contains("#")) {
                                try {
                                        int start = description.indexOf("#") + 1;
                                        int end = description.indexOf(" ", start);
                                        if (end == -1)
                                                end = description.length();
                                        idGit = Long.parseLong(description.substring(start, end));
                                } catch (Exception e) {
                                }
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

                        boolean subfaseValida = subfaseNormalizada != null && subValidas.contains(subfaseNormalizada);
                        boolean tareaValida = tituloNormalizado != null && tarValidas.contains(tituloNormalizado);
                        boolean idValido = idGit != -1 && ids.contains(idGit);

                        boolean esValida = formatoCorrecto && subfaseValida && tareaValida && idValido;

                        if (!esValida)
                                continue;

                        if (!subfaseNormalizada.equals(subfaseBuscada))
                                continue;

                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        String nombreTag = null;
                        if (tagIds != null && !tagIds.isEmpty()) {
                                nombreTag = etiquetas.get(tagIds.get(0));
                        }

                        Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                        String duration = (String) timeInterval.get("duration");
                        double horas = convertirDuracionAHoras(duration);

                        String startStr = (String) timeInterval.get("start");
                        String endStr = (String) timeInterval.get("end");

                        Date fecha = null;
                        double inicio = 0;
                        double fin = 0;

                        try {
                                if (startStr != null) {
                                        Instant start = Instant.parse(startStr);
                                        ZonedDateTime zdtStart = start.atZone(ZoneId.systemDefault());
                                        fecha = Date.from(start);
                                        inicio = zdtStart.getHour() + (zdtStart.getMinute() / 60.0);
                                }

                                if (endStr != null) {
                                        Instant end = Instant.parse(endStr);
                                        ZonedDateTime zdtEnd = end.atZone(ZoneId.systemDefault());
                                        fin = zdtEnd.getHour() + (zdtEnd.getMinute() / 60.0);
                                }
                        } catch (Exception e) {
                        }

                        if (tareasAgrupadas.containsKey(description)) {
                                ClockifyTareaDTO dtoExistente = tareasAgrupadas.get(description);
                                dtoExistente.setHorasTrabajadas(dtoExistente.getHorasTrabajadas() + horas);
                        } else {
                                ClockifyTareaDTO nuevoDto = new ClockifyTareaDTO(
                                                titulo,
                                                horas,
                                                (int) idGit,
                                                nombreTag,
                                                description,
                                                fecha,
                                                inicio,
                                                fin);
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

                ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                                clockify.getUrlReal() + "/user",
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });

                String userId = (String) userResponse.getBody().get("id");

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

                        if (!clockifyId.equals(entry.get("projectId")))
                                continue;

                        String description = (String) entry.get("description");
                        if (description == null || description.isEmpty())
                                continue;

                        boolean formatoCorrecto = description.matches("^\\[.+\\]#\\d+\\s.+$");

                        String subfase = null;
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(
                                                description.indexOf("[") + 1,
                                                description.indexOf("]"));
                        }

                        String subfaseNormalizada = subfase != null
                                        ? detalleEstimacionService.normalizarTexto(subfase)
                                        : null;

                        long idGit = -1;
                        if (description.contains("#")) {
                                try {
                                        int start = description.indexOf("#") + 1;
                                        int end = description.indexOf(" ", start);
                                        if (end == -1)
                                                end = description.length();
                                        idGit = Long.parseLong(description.substring(start, end));
                                } catch (Exception e) {
                                }
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

                        boolean tareaValida = tituloNormalizado != null && tarValidas.contains(tituloNormalizado);
                        boolean subfaseValida = subfaseNormalizada != null && subValidas.contains(subfaseNormalizada);
                        boolean idValido = idGit != -1 && ids.contains(idGit);

                        boolean esValida = formatoCorrecto && subfaseValida && idValido && tareaValida;

                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        String nombreTag = null;
                        if (tagIds != null && !tagIds.isEmpty()) {
                                nombreTag = etiquetas.get(tagIds.get(0));
                        }

                        if (!esValida) {

                                Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                                String duration = (String) timeInterval.get("duration");
                                double horas = convertirDuracionAHoras(duration);

                                String startStr = (String) timeInterval.get("start");
                                String endStr = (String) timeInterval.get("end");

                                Date fecha = null;
                                double inicio = 0;
                                double fin = 0;

                                try {
                                        if (startStr != null) {
                                                Instant start = Instant.parse(startStr);
                                                ZonedDateTime zdtStart = start.atZone(ZoneId.systemDefault());
                                                fecha = Date.from(start);
                                                inicio = zdtStart.getHour() + (zdtStart.getMinute() / 60.0);
                                        }

                                        if (endStr != null) {
                                                Instant end = Instant.parse(endStr);
                                                ZonedDateTime zdtEnd = end.atZone(ZoneId.systemDefault());
                                                fin = zdtEnd.getHour() + (zdtEnd.getMinute() / 60.0);
                                        }
                                } catch (Exception e) {
                                }

                                ClockifyTareaDTO dto = new ClockifyTareaDTO(
                                                description,
                                                horas,
                                                0,
                                                nombreTag,
                                                description,
                                                fecha,
                                                inicio,
                                                fin);

                                tareasInvalidas.add(dto);
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

        /**
         * Sincroniza las entradas de tiempo de Clockify con nuestra base de datos
         * local.
         * 
         * @param projectId ID de nuestro proyecto local.
         * @return Número de imputaciones nuevas guardadas.
         */
        @Auditable(accion = "SINCRONIZAR_CLOCKIFY", tabla = "proyecto", entidad = Proyecto.class, descripcion = "Se sincronizaron las imputaciones de Clockify para el proyecto con ID: #{#projectId}")
        public int sincronizarImputaciones(Long projectId) {
                Proyecto p = proyectoRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

                Excel excelVigente = excelRepository.findFirstByIdProyectoAndVigenteTrue(projectId);
                String clockifyId = p.getClockifyId();
                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // 1. Obtener ID del usuario maestro
                ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                                clockify.getUrlReal() + "/user", HttpMethod.GET, entity,
                                new ParameterizedTypeReference<Map<String, Object>>() {
                                });
                String userId = (String) userResponse.getBody().get("id");

                // -- OBTENER ETIQUETAS DE CLOCKIFY --
                String tagsUrl = clockify.getUrlReal() + "/workspaces/" + workspaceId + "/tags";
                ResponseEntity<List<Map<String, Object>>> tagsResponse = restTemplate.exchange(
                                tagsUrl, HttpMethod.GET, entity,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                });

                Map<String, String> mapaEtiquetas = new HashMap<>();
                if (tagsResponse.getBody() != null) {
                        for (Map<String, Object> tag : tagsResponse.getBody()) {
                                mapaEtiquetas.put((String) tag.get("id"), (String) tag.get("name"));
                        }
                }

                // Obtener listas de BD local
                List<Departamento> departamentosBD = departamentoRepository.findAll();
                List<com.example.demo.entity.Fase> todasLasFasesBD = faseRepository.findAll();

                // 2. Obtener TODAS las entradas de tiempo mediante PAGINACIÓN DINÁMICA
                List<Map<String, Object>> entradasClockify = new ArrayList<>();
                int paginaActual = 1;
                int tamanoPagina = 100;
                boolean hayMasPaginas = true;

                while (hayMasPaginas) {
                        String url = clockify.getUrlReal() + "/workspaces/" + workspaceId +
                                        "/user/" + userId + "/time-entries?page=" + paginaActual + "&page-size="
                                        + tamanoPagina;

                        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                                        url, HttpMethod.GET, entity,
                                        new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                        });

                        List<Map<String, Object>> paginaResultados = response.getBody();

                        if (paginaResultados != null && !paginaResultados.isEmpty()) {
                                entradasClockify.addAll(paginaResultados);

                                if (paginaResultados.size() < tamanoPagina) {
                                        hayMasPaginas = false;
                                } else {
                                        paginaActual++;
                                }
                        } else {
                                hayMasPaginas = false;
                        }
                }

                List<ImputacionClockify> nuevasImputaciones = new ArrayList<>();

                // 3. Procesar cada entrada
                for (Map<String, Object> entry : entradasClockify) {

                        if (!clockifyId.equals(entry.get("projectId"))) {
                                continue;
                        }

                        String idEntrada = (String) entry.get("id");
                        String description = (String) entry.get("description");

                        if (description == null || description.isEmpty()) {
                                continue;
                        }

                        Map<String, Object> timeInterval = (Map<String, Object>) entry.get("timeInterval");
                        double horasClockify = convertirDuracionAHoras((String) timeInterval.get("duration"));

                        ImputacionClockify imputacionExistente = imputacionService.obtenerPorIdClockify(idEntrada);
                        ImputacionClockify imputacion;

                        // 🔥 EL ARREGLO ESTÁ AQUÍ 🔥
                        // Si ya existe, usamos esa misma para "rellenarle" los campos nuevos. Si no,
                        // creamos una nueva.
                        if (imputacionExistente != null) {
                                imputacion = imputacionExistente;
                        } else {
                                imputacion = new ImputacionClockify();
                                imputacion.setIdClockifyOriginal(idEntrada);
                        }
                        
                        imputacion.setIdClockifyOriginal(idEntrada);
                        imputacion.setIdProyecto(projectId);
                        imputacion.setDescripcionOriginal(description);
                        imputacion.setHorasTrabajadas(horasClockify);

                        String subfase = null;
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(description.indexOf("[") + 1, description.indexOf("]"));
                                imputacion.setSubfaseExtraida(detalleEstimacionService.normalizarTexto(subfase));
                        }

                        long idGit = -1;
                        if (description.contains("#")) {
                                try {
                                        int start = description.indexOf("#") + 1;
                                        int end = description.indexOf(" ", start);
                                        if (end == -1) {
                                                end = description.length();
                                        }
                                        idGit = Long.parseLong(description.substring(start, end));
                                        // Eliminamos imputacion.setIdGitlab() para aplicar la separación de
                                        // responsabilidades
                                } catch (Exception e) {
                                }
                        }

                        if (description.contains("#")) {
                                int start = description.indexOf("#");
                                int firstSpace = description.indexOf(" ", start);
                                if (firstSpace != -1) {
                                        String titulo = description.substring(firstSpace + 1).trim();
                                        imputacion.setTareaExtraida(detalleEstimacionService.normalizarTexto(titulo));
                                }
                        }

                        // -- MAPEO DE DEPARTAMENTO (TAG) --
                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        if (tagIds != null && !tagIds.isEmpty()) {
                                String idEtiqueta = tagIds.get(0);
                                String nombreEtiquetaClockify = mapaEtiquetas.get(idEtiqueta);

                                if (nombreEtiquetaClockify != null) {
                                        String etiquetaLimpia = detalleEstimacionService
                                                        .normalizarTexto(nombreEtiquetaClockify);

                                        for (Departamento depto : departamentosBD) {
                                                if (detalleEstimacionService.normalizarTexto(depto.getNombre())
                                                                .equals(etiquetaLimpia)) {
                                                        imputacion.setIdDepartamento(depto.getId());
                                                        break;
                                                }
                                        }
                                }
                        }

                        // -- FECHAS --
                        try {
                                String startStr = (String) timeInterval.get("start");
                                if (startStr != null) {
                                        java.time.Instant start = java.time.Instant.parse(startStr);
                                        java.time.ZonedDateTime zdtStart = start
                                                        .atZone(java.time.ZoneId.systemDefault());
                                        imputacion.setFecha(zdtStart.toLocalDate());
                                        imputacion.setHoraInicio(zdtStart.toLocalTime());
                                }
                                String endStr = (String) timeInterval.get("end");
                                if (endStr != null) {
                                        java.time.Instant end = java.time.Instant.parse(endStr);
                                        java.time.ZonedDateTime zdtEnd = end.atZone(java.time.ZoneId.systemDefault());
                                        imputacion.setHoraFin(zdtEnd.toLocalTime());
                                }
                        } catch (Exception e) {
                                imputacion.setFecha(java.time.LocalDate.now());
                        }

                        // -- CRUCE POR TEXTO Y APRENDIZAJE DE ID --
                        boolean esValida = false;
                        String subfaseLimpia = imputacion.getSubfaseExtraida();
                        String tareaLimpia = imputacion.getTareaExtraida();

                        if (subfaseLimpia != null && !subfaseLimpia.isEmpty() && tareaLimpia != null
                                        && !tareaLimpia.isEmpty() && excelVigente != null) {

                                Integer idFaseEncontrada = todasLasFasesBD.stream()
                                                .filter(f -> f.getFasePadre() != null && detalleEstimacionService
                                                                .normalizarTexto(f.getNombre()).equals(subfaseLimpia))
                                                .map(com.example.demo.entity.Fase::getId)
                                                .findFirst()
                                                .orElse(null);

                                if (idFaseEncontrada != null && imputacion.getIdDepartamento() != null) {
                                        DetalleEstimacion estimacion = detalleEstimacionRepository
                                                        .findFirstByIdExcelAndIdFaseAndTareaIgnoreCaseAndIdDepartamento(
                                                                        excelVigente.getIdExcel(),
                                                                        idFaseEncontrada,
                                                                        tareaLimpia,
                                                                        imputacion.getIdDepartamento());

                                        if (estimacion != null) {
                                                imputacion.setIdDetalleEstimacion(estimacion.getId());
                                                esValida = true;

                                                if ((estimacion.getNumeroGitlab() == null
                                                                || estimacion.getNumeroGitlab().trim().isEmpty())
                                                                && idGit != -1) {
                                                        estimacion.setNumeroGitlab(String.valueOf(idGit));
                                                        detalleEstimacionRepository.save(estimacion);
                                                }
                                        }
                                }
                        }

                        imputacion.setValida(esValida);
                        nuevasImputaciones.add(imputacion);
                }

                if (!nuevasImputaciones.isEmpty()) {
                        imputacionService.guardarImputaciones(nuevasImputaciones);
                }

                return nuevasImputaciones.size();
        }
}