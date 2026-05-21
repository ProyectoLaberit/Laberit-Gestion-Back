package com.example.demo.services;

import com.example.demo.dto.AuditLogDTO;
import com.example.demo.entity.AuditLog;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extrae el ID del usuario logueado en este momento.
     * Si no hay nadie, devuelve el ID 1 por defecto.
     */
    public Integer getIdUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
            return usuarioRepository.findByEmail(auth.getName())
                    .map(Usuario::getId)
                    .orElse(1);
        }
        return 1;
    }

    public List<AuditLogDTO> obtenerTodos() {
        return auditLogRepository.findAllByOrderByFechaHoraDesc().stream()
                .map(this::mapearLog)
                .collect(Collectors.toList());
    }

    private AuditLogDTO mapearLog(AuditLog log) {
        AuditLogDTO dto = new AuditLogDTO();
        dto.setId(log.getId());
        dto.setAccion(log.getAccion());
        dto.setDescripcion(log.getDescripcion());
        dto.setFechaHora(log.getFechaHora());
        dto.setIdUsuarioActor(log.getIdUsuario());

        usuarioRepository.findById(log.getIdUsuario()).ifPresent(actor -> {
            dto.setUsuarioNombre(actor.getNombre());
            dto.setUsuarioEmail(actor.getEmail());
        });

        ResumenUsuario objetivo = resolverUsuarioObjetivo(log);
        if (objetivo != null) {
            dto.setIdUsuarioObjetivo(objetivo.id());
            dto.setUsuarioObjetivoNombre(objetivo.nombre());
            dto.setUsuarioObjetivoEmail(objetivo.email());
        }else{
            dto.setIdUsuarioObjetivo(log.getIdAfectado().intValue());
        }

        return dto;
    }

    private ResumenUsuario resolverUsuarioObjetivo(AuditLog log) {
        if (!"usuario".equalsIgnoreCase(log.getTablaAfectada())) {
            return null;
        }

        Integer idObjetivo = normalizarId(log.getIdAfectado());
        if (idObjetivo != null) {
            Optional<Usuario> usuarioObjetivo = usuarioRepository.findById(idObjetivo);
            if (usuarioObjetivo.isPresent()) {
                Usuario usuario = usuarioObjetivo.get();
                return new ResumenUsuario(usuario.getId(), usuario.getNombre(), usuario.getEmail());
            }
        }

        ResumenUsuario desdeDatosNuevos = extraerUsuarioDesdeJson(log.getDatosNuevos());
        if (desdeDatosNuevos != null) {
            return completarIdSiFalta(desdeDatosNuevos, idObjetivo);
        }

        ResumenUsuario desdeDatosPrevios = extraerUsuarioDesdeJson(log.getDatosPrevios());
        if (desdeDatosPrevios != null) {
            return completarIdSiFalta(desdeDatosPrevios, idObjetivo);
        }

        if (idObjetivo != null) {
            return new ResumenUsuario(idObjetivo, null, null);
        }

        return null;
    }

    private ResumenUsuario completarIdSiFalta(ResumenUsuario resumen, Integer idObjetivo) {
        if (resumen.id() != null || idObjetivo == null) {
            return resumen;
        }
        return new ResumenUsuario(idObjetivo, resumen.nombre(), resumen.email());
    }

    private ResumenUsuario extraerUsuarioDesdeJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            Integer id = obtenerEntero(node, "id", "idUsuario", "id_usuario");
            String nombre = obtenerTexto(node, "nombre", "usuarioNombre");
            String email = obtenerTexto(node, "email", "usuarioEmail");

            if (id == null && (nombre == null || nombre.isBlank()) && (email == null || email.isBlank())) {
                return null;
            }

            return new ResumenUsuario(id, nombre, email);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer obtenerEntero(JsonNode node, String... campos) {
        for (String campo : campos) {
            JsonNode valor = node.get(campo);
            if (valor == null || valor.isNull()) {
                continue;
            }

            if (valor.isInt() || valor.isLong()) {
                return valor.intValue();
            }

            if (valor.isTextual()) {
                try {
                    return Integer.parseInt(valor.asText());
                } catch (NumberFormatException ignored) {
                    // Seguimos con el siguiente campo.
                }
            }
        }
        return null;
    }

    private String obtenerTexto(JsonNode node, String... campos) {
        for (String campo : campos) {
            JsonNode valor = node.get(campo);
            if (valor == null || valor.isNull()) {
                continue;
            }

            String texto = valor.asText(null);
            if (texto != null && !texto.isBlank()) {
                return texto;
            }
        }
        return null;
    }

    private Integer normalizarId(Long idAfectado) {
        if (idAfectado == null || idAfectado <= 0 || idAfectado > Integer.MAX_VALUE) {
            return null;
        }
        return idAfectado.intValue();
    }

    private record ResumenUsuario(Integer id, String nombre, String email) {
    }
}
