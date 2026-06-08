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

        @Autowired
    private com.example.demo.repository.ImputacionClockifyRepository imputacionClockifyRepository;

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

        ResponseEntity<Map<String, Object>> userResponse = restTemplate.exchange(
                clockify.getUrlReal() + "/user", HttpMethod.GET, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        String userId = (String) userResponse.getBody().get("id");

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

        List<Departamento> departamentosBD = departamentoRepository.findAll();
        List<com.example.demo.entity.Fase> todasLasFasesBD = faseRepository.findAll();

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
                if (Math.abs(imputacionExistente.getHorasTrabajadas() - horasClockify) > 0.01) {
                    imputacionExistente.setHorasTrabajadas(horasClockify);
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
                String subfaseLimpiaStr = detalleEstimacionService.normalizarTexto(subfase).replaceAll("\\s+", " ");
                imputacion.setSubfaseExtraida(subfaseLimpiaStr);
            }

            if (description.contains("#")) {
                int start = description.indexOf("#");
                int firstSpace = description.indexOf(" ", start);
                if (firstSpace != -1) {
                    String numeroStr = description.substring(start + 1, firstSpace).trim();
                    try {
                        imputacion.setNumeroGitlab(Long.parseLong(numeroStr));
                    } catch (NumberFormatException e) {
                        imputacion.setNumeroGitlab(null);
                    }

                    String titulo = description.substring(firstSpace + 1).trim();
                    String tareaLimpiaStr = detalleEstimacionService.normalizarTexto(titulo).replaceAll("\\s+", " ");
                    imputacion.setTareaExtraida(tareaLimpiaStr);
                }
            }

            List<String> tagIds = (List<String>) entry.get("tagIds");
            if (tagIds != null && !tagIds.isEmpty()) {
                for (String idEtiqueta : tagIds) {
                    String nombreEtiquetaClockify = mapaEtiquetas.get(idEtiqueta);

                    if (nombreEtiquetaClockify != null) {
                        String etiquetaLimpia = detalleEstimacionService.normalizarTexto(nombreEtiquetaClockify).replaceAll("\\s+", " ");

                        for (Departamento depto : departamentosBD) {
                            String deptoLimpio = detalleEstimacionService.normalizarTexto(depto.getNombre()).replaceAll("\\s+", " ");
                            if (deptoLimpio.equals(etiquetaLimpia)) {
                                imputacion.setIdDepartamento(depto.getId());
                                break;
                            }
                        }
                    }
                    if (imputacion.getIdDepartamento() != null) {
                        break;
                    }
                }
            }

            try {
                String startStr = (String) timeInterval.get("start");
                if (startStr != null) {
                    java.time.Instant start = java.time.Instant.parse(startStr);
                    java.time.ZonedDateTime zdtStart = start.atZone(java.time.ZoneId.systemDefault());
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

            boolean esValida = false;
            String subfaseLimpia = imputacion.getSubfaseExtraida();
            String tareaLimpia = imputacion.getTareaExtraida();

            if (subfaseLimpia != null && !subfaseLimpia.isEmpty() && tareaLimpia != null && !tareaLimpia.isEmpty()) {

                Integer idFaseEncontrada = todasLasFasesBD.stream()
                        .filter(f -> {
                            if (f.getFasePadre() == null) {
                                return false;
                            }
                            String faseLimpia = detalleEstimacionService.normalizarTexto(f.getNombre()).replaceAll("\\s+", " ");
                            return faseLimpia.equals(subfaseLimpia);
                        })
                        .map(com.example.demo.entity.Fase::getId)
                        .findFirst()
                        .orElse(null);

                if (idFaseEncontrada != null && imputacion.getIdDepartamento() != null) {
                    List<com.example.demo.entity.TareaProyecto> tareasDelDepto = tareaProyectoRepository.findByIdProyecto(projectId);

                    java.util.Optional<com.example.demo.entity.TareaProyecto> tareaOpt = tareasDelDepto.stream()
                            .filter(t -> {
                                return t.getIdFase() != null && t.getIdFase().equals(idFaseEncontrada);
                            })
                            .filter(t -> {
                                return t.getIdDepartamento() != null && t.getIdDepartamento().equals(imputacion.getIdDepartamento());
                            })
                            .filter(t -> {
                                if (t.getTarea() == null) {
                                    return false;
                                }
                                String tLimpia = detalleEstimacionService.normalizarTexto(t.getTarea()).replaceAll("\\s+", " ");
                                return tLimpia.equals(tareaLimpia);
                            })
                            .findFirst();

                    if (tareaOpt.isPresent()) {
                        imputacion.setIdTareaProyecto(tareaOpt.get().getIdTareaProyecto());
                        esValida = true;
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



   /**
     * Busca las imputaciones de Clockify huérfanas (inválidas) de un proyecto y trata de vincularlas
     * con las tareas locales actualizadas.
     * Primero intenta una vía rápida mediante el número de GitLab. Si falla, coteja por texto exacto.
     *
     * @param idProyecto Identificador único del proyecto local cuyas imputaciones se van a revisar.
     */
    public void revincularClockifyHuerfanas(Long idProyecto) {
        List<ImputacionClockify> huerfanas = imputacionClockifyRepository.findByIdProyectoAndValida(idProyecto, false);
        if (huerfanas.isEmpty()) {
            return;
        }

        // 1. Preparamos el mapa de tareas de GitLab válidas para la "vía rápida"
        List<GitLabTarea> tareasGitLabBD = gitLabTareaRepository.findByIdProyecto(idProyecto);
        Map<Long, Long> mapaGitLab = new HashMap<>();
        if (tareasGitLabBD != null) {
            for (GitLabTarea gt : tareasGitLabBD) {
                if (gt.getValida() && gt.getNumeroGitlab() != null && gt.getTareaProyecto() != null) {
                    mapaGitLab.put(gt.getNumeroGitlab(), gt.getTareaProyecto());
                }
            }
        }

        List<com.example.demo.entity.Fase> todasLasFasesBD = faseRepository.findAll();
        List<TareaProyecto> tareasDelProyecto = tareaProyectoRepository.findByIdProyecto(idProyecto);
        List<ImputacionClockify> actualizadas = new ArrayList<>();

        for (ImputacionClockify imputacion : huerfanas) {
            
            // --- VÍA RÁPIDA: Por número de GitLab ---
            if (imputacion.getNumeroGitlab() != null && mapaGitLab.containsKey(imputacion.getNumeroGitlab())) {
                imputacion.setIdTareaProyecto(mapaGitLab.get(imputacion.getNumeroGitlab()));
                imputacion.setValida(true);
                actualizadas.add(imputacion);
                continue; // Pasamos directamente a la siguiente imputación
            }

            // --- VÍA TEXTO: Por subfase, tarea y departamento ---
            String subfaseLimpia = imputacion.getSubfaseExtraida();
            String tareaLimpia = imputacion.getTareaExtraida();

            if (subfaseLimpia != null && !subfaseLimpia.isEmpty() && tareaLimpia != null && !tareaLimpia.isEmpty()) {
                Integer idFaseEncontrada = todasLasFasesBD.stream()
                        .filter(f -> {
                            if (f.getFasePadre() == null) {
                                return false;
                            }
                            String faseLimpia = detalleEstimacionService.normalizarTexto(f.getNombre()).replaceAll("\\s+", " ");
                            return faseLimpia.equals(subfaseLimpia);
                        })
                        .map(com.example.demo.entity.Fase::getId)
                        .findFirst()
                        .orElse(null);

                if (idFaseEncontrada != null && imputacion.getIdDepartamento() != null) {
                    java.util.Optional<TareaProyecto> tareaOpt = tareasDelProyecto.stream()
                            .filter(t -> {
                                return t.getIdFase() != null && t.getIdFase().equals(idFaseEncontrada);
                            })
                            .filter(t -> {
                                return t.getIdDepartamento() != null && t.getIdDepartamento().equals(imputacion.getIdDepartamento());
                            })
                            .filter(t -> {
                                if (t.getTarea() == null) {
                                    return false;
                                }
                                String tLimpia = detalleEstimacionService.normalizarTexto(t.getTarea()).replaceAll("\\s+", " ");
                                return tLimpia.equals(tareaLimpia);
                            })
                            .findFirst();

                    if (tareaOpt.isPresent()) {
                        imputacion.setIdTareaProyecto(tareaOpt.get().getIdTareaProyecto());
                        imputacion.setValida(true);
                        actualizadas.add(imputacion);
                    }
                }
            }
        }

        if (!actualizadas.isEmpty()) {
            imputacionClockifyRepository.saveAll(actualizadas);
        }
    }
      

       
}
