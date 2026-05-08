package com.example.demo.services;

import com.example.demo.entity.AuditLog;
import com.example.demo.entity.Usuario;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ── Constantes de acción ──────────────────────────────────────────────────
    public static final String IMPORTACION_EXCEL   = "IMPORTACION_EXCEL";
    public static final String CAMBIO_ESTIMACION   = "CAMBIO_ESTIMACION";
    public static final String CREACION_ESTIMACION = "CREACION_ESTIMACION";
    public static final String BORRADO_ESTIMACION  = "BORRADO_ESTIMACION";
    public static final String SINCRONIZACION      = "SINCRONIZACION";
    public static final String CREACION_USUARIO    = "CREACION_USUARIO";
    public static final String BORRADO_USUARIO     = "BORRADO_USUARIO";
    public static final String CAMBIO_ROL          = "CAMBIO_ROL";

    // ── Registro de eventos ───────────────────────────────────────────────────

    /**
     * Registra un evento de auditoría obteniendo el usuario del contexto de seguridad.
     */
    public void registrar(String accion, String descripcion, Long idProyecto) {
        registrar(accion, descripcion, idProyecto, null);
    }

    /**
     * Registra un evento indicando también el usuario afectado por la acción.
     */
    public void registrar(String accion, String descripcion, Long idProyecto, Integer idUsuarioObjetivo) {
        try {
            Usuario usuarioActual = getUsuarioActual();
            String email  = usuarioActual != null ? usuarioActual.getEmail() : getEmailActual();
            String nombre = usuarioActual != null && usuarioActual.getNombre() != null
                    ? usuarioActual.getNombre()
                    : email;
            Integer idUsuario = usuarioActual != null ? usuarioActual.getId() : null;
            AuditLog log = new AuditLog(accion, descripcion, idUsuario, email, nombre, idProyecto, idUsuarioObjetivo);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // El logging nunca debe romper el flujo principal
            System.err.println("[AUDIT ERROR] No se pudo guardar el log: " + e.getMessage());
        }
    }

    /**
     * Registra un evento con nombre de usuario explícito (cuando ya lo tienes disponible).
     */
    public void registrar(String accion, String descripcion, String usuarioEmail,
                          String usuarioNombre, Long idProyecto) {
        registrar(accion, descripcion, usuarioEmail, usuarioNombre, idProyecto, null);
    }

    public void registrar(String accion, String descripcion, String usuarioEmail,
                          String usuarioNombre, Long idProyecto, Integer idUsuarioObjetivo) {
        try {
            Integer idUsuario = usuarioRepository.findByEmail(usuarioEmail)
                    .map(Usuario::getId)
                    .orElse(null);
            AuditLog log = new AuditLog(accion, descripcion, idUsuario, usuarioEmail, usuarioNombre, idProyecto, idUsuarioObjetivo);
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[AUDIT ERROR] No se pudo guardar el log: " + e.getMessage());
        }
    }

    // ── Consultas (solo SuperAdministrador) ───────────────────────────────────

    public List<AuditLog> obtenerTodos() {
        return auditLogRepository.findAllByOrderByFechaHoraDesc();
    }

    public List<AuditLog> obtenerPorProyecto(Long idProyecto) {
        return auditLogRepository.findByIdProyectoOrderByFechaHoraDesc(idProyecto);
    }

    public List<AuditLog> obtenerPorUsuario(String email) {
        return auditLogRepository.findByUsuarioEmailOrderByFechaHoraDesc(email);
    }

    public List<AuditLog> obtenerPorUsuario(Integer idUsuario) {
        return auditLogRepository.findByIdUsuarioOrderByFechaHoraDesc(idUsuario);
    }

    public List<AuditLog> obtenerPorAccion(String accion) {
        return auditLogRepository.findByAccionOrderByFechaHoraDesc(accion);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getEmailActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "sistema";
    }

    private Usuario getUsuarioActual() {
        String email = getEmailActual();
        return usuarioRepository.findByEmail(email).orElse(null);
    }
}
