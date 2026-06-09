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

        /**
     * Obtiene y agrupa las imputaciones válidas de una subfase leyendo exclusivamente de la base de datos local.
     */
   public List<ClockifyTareaDTO> obtenerTareasPorSubfase(Long projectId, String subfas) {
        String subfaseBuscada = detalleEstimacionService.normalizarTexto(subfas);
        List<ImputacionClockify> imputacionesValidas = imputacionClockifyRepository.findByIdProyectoAndValida(projectId, true);
        Map<String, ClockifyTareaDTO> tareasAgrupadas = new HashMap<>();

        for (ImputacionClockify imp : imputacionesValidas) {
            // Extraer subfase dinámicamente del string original por si la BD tiene nulos
            String subfaseImputacion = null;
            String desc = imp.getDescripcionOriginal();
            if (desc != null && desc.contains("[") && desc.contains("]")) {
                subfaseImputacion = desc.substring(desc.indexOf("[") + 1, desc.indexOf("]"));
            }
            subfaseImputacion = detalleEstimacionService.normalizarTexto(subfaseImputacion);

            if (!subfaseBuscada.equals(subfaseImputacion)) {
                continue;
            }

            String nombreTag = null;
            if (imp.getIdDepartamento() != null) {
                nombreTag = departamentoRepository.findById(imp.getIdDepartamento())
                        .map(Departamento::getNombre)
                        .orElse(null);
            }

            java.util.Date fecha = imp.getFecha() != null ? java.sql.Date.valueOf(imp.getFecha()) : null;
            double inicio = imp.getHoraInicio() != null ? imp.getHoraInicio().getHour() + (imp.getHoraInicio().getMinute() / 60.0) : 0.0;
            double fin = imp.getHoraFin() != null ? imp.getHoraFin().getHour() + (imp.getHoraFin().getMinute() / 60.0) : 0.0;

            String key = imp.getDescripcionOriginal();

            if (tareasAgrupadas.containsKey(key)) {
                ClockifyTareaDTO dtoExistente = tareasAgrupadas.get(key);
                double horasExtra = imp.getHorasTrabajadas() != null ? imp.getHorasTrabajadas() : 0.0;
                dtoExistente.setHorasTrabajadas(dtoExistente.getHorasTrabajadas() + horasExtra);
            } else {
                ClockifyTareaDTO nuevoDto = new ClockifyTareaDTO(
                        extraerNombreVisual(imp.getDescripcionOriginal()), // Texto limpio
                        imp.getHorasTrabajadas() != null ? imp.getHorasTrabajadas() : 0.0,
                        nombreTag,
                        imp.getDescripcionOriginal(),
                        fecha,
                        inicio,
                        fin
                );
                tareasAgrupadas.put(key, nuevoDto);
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

       /**
     * Obtiene las imputaciones marcadas como inválidas directamente desde la base de datos local.
     */
   public List<ClockifyTareaDTO> obtenerEntradasInvalidas(Long projectId) {
        // Leemos 100% de la Base de Datos Local
        List<ImputacionClockify> invalidasBD = imputacionClockifyRepository.findByIdProyectoAndValidaFalse(projectId);
        List<ClockifyTareaDTO> tareasInvalidas = new ArrayList<>();

        for (ImputacionClockify imp : invalidasBD) {
            String nombreTag = null;
            if (imp.getIdDepartamento() != null) {
                nombreTag = departamentoRepository.findById(imp.getIdDepartamento())
                        .map(Departamento::getNombre)
                        .orElse(null);
            }

            java.util.Date fecha = imp.getFecha() != null ? java.sql.Date.valueOf(imp.getFecha()) : null;
            double inicio = imp.getHoraInicio() != null ? imp.getHoraInicio().getHour() + (imp.getHoraInicio().getMinute() / 60.0) : 0.0;
            double fin = imp.getHoraFin() != null ? imp.getHoraFin().getHour() + (imp.getHoraFin().getMinute() / 60.0) : 0.0;

            ClockifyTareaDTO dto = new ClockifyTareaDTO(
                    extraerNombreVisual(imp.getDescripcionOriginal()), // Aquí extraemos solo el texto limpio para la vista
                    imp.getHorasTrabajadas() != null ? imp.getHorasTrabajadas() : 0.0,
                    nombreTag,
                    imp.getDescripcionOriginal(), // Mantenemos el string original intacto
                    fecha,
                    inicio,
                    fin
            );
            tareasInvalidas.add(dto);
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
   /**
     * Sincroniza las entradas de tiempo de Clockify con nuestra base de datos local.
     * Incorpora extracción de horas/fechas limpia y doble vía de validación (GitLab + Texto).
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
                new ParameterizedTypeReference<Map<String, Object>>() {});
        String userId = (String) userResponse.getBody().get("id");

        String tagsUrl = clockify.getUrlReal() + "/workspaces/" + workspaceId + "/tags";
        ResponseEntity<List<Map<String, Object>>> tagsResponse = restTemplate.exchange(
                tagsUrl, HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {});

        Map<String, String> mapaEtiquetas = new HashMap<>();
        if (tagsResponse.getBody() != null) {
            for (Map<String, Object> tag : tagsResponse.getBody()) {
                mapaEtiquetas.put((String) tag.get("id"), (String) tag.get("name"));
            }
        }

        List<Departamento> departamentosBD = departamentoRepository.findAll();
        List<TareaProyecto> tareasDelProyecto = tareaProyectoRepository.findByIdProyecto(projectId);

        List<Map<String, Object>> entradasClockify = new ArrayList<>();
        int paginaActual = 1;
        int tamanoPagina = 100;
        boolean hayMasPaginas = true;

        while (hayMasPaginas) {
            String url = clockify.getUrlReal() + "/workspaces/" + workspaceId +
                    "/user/" + userId + "/time-entries?page=" + paginaActual + "&page-size=" + tamanoPagina;

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            List<Map<String, Object>> paginaResultados = response.getBody();
            if (paginaResultados != null && !paginaResultados.isEmpty()) {
                entradasClockify.addAll(paginaResultados);
                hayMasPaginas = (paginaResultados.size() == tamanoPagina);
                paginaActual++;
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

            ImputacionClockify imputacion = imputacionService.obtenerPorIdClockify(idEntrada);
            boolean esNuevo = (imputacion == null);

            if (esNuevo) {
                imputacion = new ImputacionClockify();
                imputacion.setIdClockifyOriginal(idEntrada);
                imputacion.setIdProyecto(projectId);
            }

            imputacion.setDescripcionOriginal(description);
            imputacion.setHorasTrabajadas(horasClockify);

            // 1. ASIGNACIÓN DE FECHA Y HORA BÁSICA
            try {
                String startStr = (String) timeInterval.get("start");
                if (startStr != null) {
                    java.time.Instant start = java.time.Instant.parse(startStr);
                    java.time.ZonedDateTime zdtStart = start.atZone(java.time.ZoneId.systemDefault());
                    imputacion.setFecha(zdtStart.toLocalDate());
                    imputacion.setHoraInicio(zdtStart.toLocalTime());
                } else {
                    imputacion.setFecha(java.time.LocalDate.now());
                }
                
                String endStr = (String) timeInterval.get("end");
                if (endStr != null) {
                    java.time.Instant end = java.time.Instant.parse(endStr);
                    java.time.ZonedDateTime zdtEnd = end.atZone(java.time.ZoneId.systemDefault());
                    imputacion.setHoraFin(zdtEnd.toLocalTime());
                }
            } catch (Exception e) {
                if (imputacion.getFecha() == null) {
                    imputacion.setFecha(java.time.LocalDate.now());
                }
            }

            // 2. EXTRACCIÓN DE DEPARTAMENTO
            List<String> tagIds = (List<String>) entry.get("tagIds");
            if (tagIds != null) {
                for (String idEtiqueta : tagIds) {
                    String nombreTag = mapaEtiquetas.get(idEtiqueta);
                    if (nombreTag != null) {
                        String etiquetaLimpia = detalleEstimacionService.normalizarTexto(nombreTag).replaceAll("\\s+", " ");
                        for (Departamento depto : departamentosBD) {
                            if (detalleEstimacionService.normalizarTexto(depto.getNombre()).replaceAll("\\s+", " ").equals(etiquetaLimpia)) {
                                imputacion.setIdDepartamento(depto.getId());
                                break;
                            }
                        }
                    }
                }
            }

            // 3. EXTRACCIÓN DIVIDIDA DE SUBFASE, NÚMERO DE GITLAB Y TAREA
            String subfaseExtraida = null;
            if (description.contains("[") && description.contains("]")) {
                String subfase = description.substring(description.indexOf("[") + 1, description.indexOf("]"));
                subfaseExtraida = detalleEstimacionService.normalizarTexto(subfase).replaceAll("\\s+", " ");
                imputacion.setSubfaseExtraida(subfaseExtraida);
            } else {
                imputacion.setSubfaseExtraida(null);
            }

            String tareaExtraida = null;
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
                    tareaExtraida = detalleEstimacionService.normalizarTexto(titulo).replaceAll("\\s+", " ");
                    imputacion.setTareaExtraida(tareaExtraida);
                } else {
                    imputacion.setTareaExtraida(null);
                }
            } else {
                imputacion.setTareaExtraida(null);
            }

            // 4. COMPARACIÓN BÁSICA: STRING DE CLOCKIFY VS STRING DE TAREA_PROYECTO
            boolean esValida = false;
            final Integer idDepartamento = imputacion.getIdDepartamento();

            if (tareaExtraida != null && idDepartamento != null) {
                final String tareaBuscadaFinal = tareaExtraida;
                java.util.Optional<TareaProyecto> coincidencia = tareasDelProyecto.stream()
                        .filter(tp -> {
                            return tp.getIdDepartamento().equals(idDepartamento);
                        })
                        .filter(tp -> {
                            String tareaTp = detalleEstimacionService.normalizarTexto(tp.getTarea()).replaceAll("\\s+", " ");
                            return tareaTp.equals(tareaBuscadaFinal);
                        })
                        .findFirst();

                if (coincidencia.isPresent()) {
                    imputacion.setIdTareaProyecto(coincidencia.get().getIdTareaProyecto());
                    esValida = true;
                }
            }

            imputacion.setValida(esValida);
            if (!esValida) {
                imputacion.setIdTareaProyecto(null);
            }

            if (esNuevo) {
                nuevasImputaciones.add(imputacion);
            } else {
                imputacionService.actualizarImputacion(imputacion);
            }
        }

        if (!nuevasImputaciones.isEmpty()) {
            imputacionService.guardarImputaciones(nuevasImputaciones);
        }
        
        return nuevasImputaciones.size();
    }
    /**
     * Busca las imputaciones de Clockify huérfanas (inválidas) de un proyecto y trata de vincularlas
     * con las tareas locales actualizadas.
     */
   public void revincularClockifyHuerfanas(Long idProyecto) {
        // Reevaluación Total: Traemos TODAS las imputaciones
        List<ImputacionClockify> todas = imputacionClockifyRepository.findByIdProyecto(idProyecto);
        if (todas.isEmpty()) {
            return;
        }

        List<TareaProyecto> tareasDelProyecto = tareaProyectoRepository.findByIdProyecto(idProyecto);

        for (ImputacionClockify imputacion : todas) {
            // Leemos el string limpio directamente de la base de datos
            String tareaBuscada = imputacion.getTareaExtraida();
            Integer idDepto = imputacion.getIdDepartamento();
            boolean esValida = false;

            if (tareaBuscada != null && idDepto != null) {
                java.util.Optional<TareaProyecto> coincidencia = tareasDelProyecto.stream()
                        .filter(tp -> {
                            return tp.getIdDepartamento().equals(idDepto);
                        })
                        .filter(tp -> {
                            return detalleEstimacionService.normalizarTexto(tp.getTarea()).replaceAll("\\s+", " ").equals(tareaBuscada);
                        })
                        .findFirst();

                if (coincidencia.isPresent()) {
                    imputacion.setIdTareaProyecto(coincidencia.get().getIdTareaProyecto());
                    esValida = true;
                }
            }

            imputacion.setValida(esValida);
            if (!esValida) {
                imputacion.setIdTareaProyecto(null);
            }
        }

        imputacionClockifyRepository.saveAll(todas);
    }


   private String extraerNombreVisual(String descripcionOriginal) {
        if (descripcionOriginal == null) return "Tarea sin descripción";
        if (descripcionOriginal.contains("#")) {
            int start = descripcionOriginal.indexOf("#");
            int firstSpace = descripcionOriginal.indexOf(" ", start);
            if (firstSpace != -1) {
                return descripcionOriginal.substring(firstSpace + 1).trim();
            }
        }
        return descripcionOriginal;
    }
      

       
}
