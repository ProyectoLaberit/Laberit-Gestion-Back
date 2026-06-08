package com.example.demo.services;

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
import com.example.demo.dto.ProyectoClockifyDTO;
import com.example.demo.entity.ApiConfig;
import com.example.demo.entity.Departamento;
import com.example.demo.entity.GitLabTarea;
import com.example.demo.entity.ImputacionClockify;
import com.example.demo.entity.Proyecto;
import com.example.demo.entity.TareaProyecto;
import com.example.demo.repository.ApiConfigRepository;
import com.example.demo.repository.DepartamentoRepository;
import com.example.demo.repository.FaseRepository;
import com.example.demo.repository.GitLabTareaRepository;
import com.example.demo.repository.ProyectoRepository;

import java.time.Duration;
import java.time.Instant;
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
        private ImputacionClockifyService imputacionService;

        @Autowired
        private com.example.demo.repository.TareaProyectoRepository tareaProyectoRepository;

        @Autowired
        private GitLabTareaRepository gitLabTareaRepository;

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

                // Cargamos todas las fases para cruzar los nombres
                List<com.example.demo.entity.Fase> todasLasFases = faseRepository.findAll();

                // Cargamos las tareas del nuevo pivote
                List<com.example.demo.entity.TareaProyecto> tareasDelProyecto = tareaProyectoRepository
                                .findByIdProyecto(projectId);

                Set<String> tarValidas = tareasDelProyecto.stream()
                                .map(com.example.demo.entity.TareaProyecto::getTarea)
                                .map(detalleEstimacionService::normalizarTexto)
                                .collect(Collectors.toSet());

                Set<String> subValidas = tareasDelProyecto.stream()
                                .map(tp -> todasLasFases.stream()
                                                .filter(f -> f.getId().equals(tp.getIdFase()))
                                                .map(com.example.demo.entity.Fase::getNombre)
                                                .findFirst()
                                                .orElse(""))
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
                        boolean esValida = formatoCorrecto && subfaseValida && tareaValida;

                        if (!esValida)
                                continue;

                        if (!subfaseNormalizada.equals(subfaseBuscada))
                                continue;

                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        String nombreTag = null;
                        if (tagIds != null && !tagIds.isEmpty()) {
                                // Obtiene los nombres de todas las etiquetas y las une con comas
                                nombreTag = tagIds.stream()
                                                .map(etiquetas::get)
                                                .filter(n -> n != null)
                                                .collect(Collectors.joining(", "));
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

                // 1. Llamada a Clockify
                ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                                url,
                                HttpMethod.GET,
                                entity,
                                new ParameterizedTypeReference<List<Map<String, Object>>>() {
                                });

                List<Map<String, Object>> proyectosClockify = response.getBody();

                // 2. Proyectos en BD
                List<Proyecto> proyectosBD = proyectoRepository.findAll();

                Set<String> idsBD = proyectosBD.stream()
                                .map(Proyecto::getClockifyId)
                                .collect(Collectors.toSet());

                // 3. Construir DTOs solo de los nuevos
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

        public List<ClockifyTareaDTO> obtenerEntradasInvalidas(Long projectId) {

                Proyecto p = proyectoRepository.findById(projectId)
                                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

                String clockifyId = p.getClockifyId();
                ApiConfig clockify = repositorioApi.findByNombre("Clockify Maestro");

                Map<String, String> etiquetas = cargarMapaTagsClockify();

                HttpHeaders headers = new HttpHeaders();
                headers.set("X-Api-Key", clockify.getClave());
                HttpEntity<String> entity = new HttpEntity<>(headers);

                // Cargamos todas las fases para cruzar los nombres
                List<com.example.demo.entity.Fase> todasLasFases = faseRepository.findAll();

                // Cargamos las tareas del nuevo pivote
                List<com.example.demo.entity.TareaProyecto> tareasDelProyecto = tareaProyectoRepository
                                .findByIdProyecto(projectId);

                Set<String> tarValidas = tareasDelProyecto.stream()
                                .map(com.example.demo.entity.TareaProyecto::getTarea)
                                .map(detalleEstimacionService::normalizarTexto)
                                .collect(Collectors.toSet());

                Set<String> subValidas = tareasDelProyecto.stream()
                                .map(tp -> todasLasFases.stream()
                                                .filter(f -> f.getId().equals(tp.getIdFase()))
                                                .map(com.example.demo.entity.Fase::getNombre)
                                                .findFirst()
                                                .orElse(""))
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
                        boolean esValida = formatoCorrecto && subfaseValida && tareaValida;

                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        String nombreTag = null;
                        if (tagIds != null && !tagIds.isEmpty()) {
                                // Obtiene los nombres de todas las etiquetas y las une con comas
                                nombreTag = tagIds.stream()
                                                .map(etiquetas::get)
                                                .filter(n -> n != null)
                                                .collect(Collectors.joining(", "));
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
                List<TareaProyecto> tareasDelProyectoVigente = tareaProyectoRepository
                                .findByIdProyectoAndExcelVigente(projectId);
                if (tareasDelProyectoVigente.isEmpty()) {
                        tareasDelProyectoVigente = tareaProyectoRepository.findByIdProyecto(projectId);
                }

                Map<Long, TareaProyecto> tareasVigentesPorId = new HashMap<>();
                for (TareaProyecto tareaProyecto : tareasDelProyectoVigente) {
                        if (tareaProyecto.getIdTareaProyecto() != null) {
                                tareasVigentesPorId.put(tareaProyecto.getIdTareaProyecto(), tareaProyecto);
                        }
                }

                Map<Long, GitLabTarea> tareasGitLabValidasPorNumero = new HashMap<>();
                for (GitLabTarea tareaGitLab : gitLabTareaRepository.findValidasByProyectoIncluyendoVinculacion(projectId)) {
                        if (tareaGitLab.getNumeroGitlab() == null || tareaGitLab.getTareaProyecto() == null) {
                                continue;
                        }
                        if (!tareasVigentesPorId.containsKey(tareaGitLab.getTareaProyecto())) {
                                continue;
                        }
                        tareasGitLabValidasPorNumero.putIfAbsent(tareaGitLab.getNumeroGitlab(), tareaGitLab);
                }

                int imputacionesActualizadas = 0;

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

                        if (imputacionExistente != null) {
                                boolean requiereGuardado = false;
                                if (Math.abs(imputacionExistente.getHorasTrabajadas() - horasClockify) > 0.01) {
                                        imputacionExistente.setHorasTrabajadas(horasClockify);
                                        requiereGuardado = true;
                                }

                                Boolean validaAnterior = imputacionExistente.getValida();
                                Long idTareaAnterior = imputacionExistente.getIdTareaProyecto();
                                boolean estabaInvalida = !Boolean.TRUE.equals(validaAnterior);
                                boolean quedaValida = vincularImputacionConTareaProyecto(imputacionExistente, projectId,
                                                todasLasFasesBD, tareasDelProyectoVigente, tareasVigentesPorId,
                                                tareasGitLabValidasPorNumero);

                                if (!java.util.Objects.equals(validaAnterior, imputacionExistente.getValida())
                                                || !java.util.Objects.equals(idTareaAnterior,
                                                                imputacionExistente.getIdTareaProyecto())) {
                                        requiereGuardado = true;
                                }

                                if (estabaInvalida && quedaValida) {
                                        imputacionesActualizadas++;
                                }

                                if (requiereGuardado) {
                                        imputacionService.actualizarImputacion(imputacionExistente);
                                }
                                continue;
                        }

                        ImputacionClockify imputacion = new ImputacionClockify();
                        imputacion.setIdClockifyOriginal(idEntrada);
                        imputacion.setIdProyecto(projectId);
                        imputacion.setDescripcionOriginal(description);
                        imputacion.setHorasTrabajadas(horasClockify);

                        String subfase = null;
                        if (description.contains("[") && description.contains("]")) {
                                subfase = description.substring(description.indexOf("[") + 1, description.indexOf("]"));
                                imputacion.setSubfaseExtraida(detalleEstimacionService.normalizarTexto(subfase));
                        }

                        if (description.contains("#")) {
                                int start = description.indexOf("#");
                                int firstSpace = description.indexOf(" ", start);
                                if (firstSpace != -1) {
                                        // 1. Extraer y guardar el número de GitLab como pivote numérico
                                        String numeroStr = description.substring(start + 1, firstSpace).trim();
                                        try {
                                                imputacion.setNumeroGitlab(Long.parseLong(numeroStr));
                                        } catch (NumberFormatException e) {
                                                imputacion.setNumeroGitlab(null);
                                        }

                                        // 2. Extraer y guardar el nombre de la tarea (Mantiene intacta la auditoría
                                        // visual)
                                        String titulo = description.substring(firstSpace + 1).trim();
                                        imputacion.setTareaExtraida(detalleEstimacionService.normalizarTexto(titulo));
                                }
                        }

                        // -- MAPEO DE DEPARTAMENTO (TAG) --
                        List<String> tagIds = (List<String>) entry.get("tagIds");
                        if (tagIds != null && !tagIds.isEmpty()) {
                                // Recorre todos los tags que haya puesto el usuario en Clockify
                                for (String idEtiqueta : tagIds) {
                                        String nombreEtiquetaClockify = mapaEtiquetas.get(idEtiqueta);
                                        if (nombreEtiquetaClockify != null) {
                                                String etiquetaLimpia = detalleEstimacionService.normalizarTexto(nombreEtiquetaClockify);
                                                for (Departamento depto : departamentosBD) {
                                                        if (detalleEstimacionService.normalizarTexto(depto.getNombre()).equals(etiquetaLimpia)) {
                                                                imputacion.setIdDepartamento(depto.getId());
                                                                break; // Encuentra el departamento
                                                        }
                                                }
                                        }
                                        
                                        // Si ya logró asignarle el departamento a la imputación, para de buscar en el resto de etiquetas de esta tarea
                                        if (imputacion.getIdDepartamento() != null) {
                                                break; 
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
                                        && !tareaLimpia.isEmpty()) {

                                Integer idFaseEncontrada = todasLasFasesBD.stream()
                                                .filter(f -> f.getFasePadre() != null && detalleEstimacionService
                                                                .normalizarTexto(f.getNombre()).equals(subfaseLimpia))
                                                .map(com.example.demo.entity.Fase::getId)
                                                .findFirst()
                                                .orElse(null);

                                if (idFaseEncontrada != null && imputacion.getIdDepartamento() != null) {
                                        // Cargamos las tareas del proyecto y aplicamos normalización estricta en el
                                        // filtrado de memoria
                                        List<com.example.demo.entity.TareaProyecto> tareasDelDepto = tareaProyectoRepository
                                                        .findByIdProyecto(projectId);

                                        java.util.Optional<com.example.demo.entity.TareaProyecto> tareaOpt = tareasDelDepto
                                                        .stream()
                                                        .filter(t -> t.getIdFase() != null
                                                                        && t.getIdFase().equals(idFaseEncontrada))
                                                        .filter(t -> t.getIdDepartamento() != null
                                                                        && t.getIdDepartamento().equals(
                                                                                        imputacion.getIdDepartamento()))
                                                        .filter(t -> t.getTarea() != null && detalleEstimacionService
                                                                        .normalizarTexto(t.getTarea())
                                                                        .equals(tareaLimpia))
                                                        .findFirst();

                                        if (tareaOpt.isPresent()) {
                                                imputacion.setIdTareaProyecto(tareaOpt.get().getIdTareaProyecto());
                                                esValida = true;
                                        }
                                }
                        }

                        imputacion.setValida(vincularImputacionConTareaProyecto(imputacion, projectId,
                                        todasLasFasesBD, tareasDelProyectoVigente, tareasVigentesPorId,
                                        tareasGitLabValidasPorNumero));
                        nuevasImputaciones.add(imputacion);
                }

                if (!nuevasImputaciones.isEmpty()) {
                        imputacionService.guardarImputaciones(nuevasImputaciones);
                }

                return nuevasImputaciones.size() + imputacionesActualizadas;
        }

        private boolean vincularImputacionConTareaProyecto(
                        ImputacionClockify imputacion,
                        Long projectId,
                        List<com.example.demo.entity.Fase> todasLasFasesBD,
                        List<TareaProyecto> tareasDelProyectoVigente,
                        Map<Long, TareaProyecto> tareasVigentesPorId,
                        Map<Long, GitLabTarea> tareasGitLabValidasPorNumero) {

                Long idTareaPorGitLab = resolverTareaProyectoPorGitLab(
                                imputacion,
                                projectId,
                                tareasVigentesPorId,
                                tareasGitLabValidasPorNumero);

                if (idTareaPorGitLab != null) {
                        imputacion.setIdTareaProyecto(idTareaPorGitLab);
                        imputacion.setValida(true);
                        return true;
                }

                Long idTareaPorTexto = resolverTareaProyectoPorTexto(
                                imputacion,
                                todasLasFasesBD,
                                tareasDelProyectoVigente);

                if (idTareaPorTexto != null) {
                        imputacion.setIdTareaProyecto(idTareaPorTexto);
                        imputacion.setValida(true);
                        return true;
                }

                imputacion.setIdTareaProyecto(null);
                imputacion.setValida(false);
                return false;
        }

        private Long resolverTareaProyectoPorGitLab(
                        ImputacionClockify imputacion,
                        Long projectId,
                        Map<Long, TareaProyecto> tareasVigentesPorId,
                        Map<Long, GitLabTarea> tareasGitLabValidasPorNumero) {

                if (imputacion.getNumeroGitlab() == null) {
                        return null;
                }

                GitLabTarea tareaGitLab = tareasGitLabValidasPorNumero.get(imputacion.getNumeroGitlab());
                if (tareaGitLab == null || tareaGitLab.getTareaProyecto() == null) {
                        return null;
                }

                TareaProyecto tareaProyecto = tareasVigentesPorId.get(tareaGitLab.getTareaProyecto());
                if (tareaProyecto == null || !projectId.equals(tareaProyecto.getIdProyecto())) {
                        return null;
                }

                if (imputacion.getIdDepartamento() != null && tareaProyecto.getIdDepartamento() != null
                                && !tareaProyecto.getIdDepartamento().equals(imputacion.getIdDepartamento())) {
                        return null;
                }

                if (imputacion.getIdDepartamento() == null) {
                        imputacion.setIdDepartamento(tareaProyecto.getIdDepartamento());
                }

                return tareaProyecto.getIdTareaProyecto();
        }

        private Long resolverTareaProyectoPorTexto(
                        ImputacionClockify imputacion,
                        List<com.example.demo.entity.Fase> todasLasFasesBD,
                        List<TareaProyecto> tareasDelProyectoVigente) {

                String subfaseLimpia = imputacion.getSubfaseExtraida();
                String tareaLimpia = imputacion.getTareaExtraida();

                if (subfaseLimpia == null || subfaseLimpia.isEmpty()
                                || tareaLimpia == null || tareaLimpia.isEmpty()
                                || imputacion.getIdDepartamento() == null) {
                        return null;
                }

                Integer idFaseEncontrada = todasLasFasesBD.stream()
                                .filter(fase -> fase.getFasePadre() != null
                                                && detalleEstimacionService.normalizarTexto(fase.getNombre())
                                                                .equals(subfaseLimpia))
                                .map(com.example.demo.entity.Fase::getId)
                                .findFirst()
                                .orElse(null);

                if (idFaseEncontrada == null) {
                        return null;
                }

                return tareasDelProyectoVigente.stream()
                                .filter(tarea -> tarea.getIdFase() != null
                                                && tarea.getIdFase().equals(idFaseEncontrada))
                                .filter(tarea -> tarea.getIdDepartamento() != null
                                                && tarea.getIdDepartamento().equals(imputacion.getIdDepartamento()))
                                .filter(tarea -> tarea.getTarea() != null
                                                && detalleEstimacionService.normalizarTexto(tarea.getTarea())
                                                                .equals(tareaLimpia))
                                .map(TareaProyecto::getIdTareaProyecto)
                                .findFirst()
                                .orElse(null);
        }
}
