package com.example.demo.services;

import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

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
        try {
            String email  = getEmailActual();
            String nombre = email; // fallback si no hay más datos
            AuditLog log = new AuditLog(accion, descripcion, email, nombre, idProyecto);
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
        try {
            AuditLog log = new AuditLog(accion, descripcion, usuarioEmail, usuarioNombre, idProyecto);
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

    public List<AuditLog> obtenerPorAccion(String accion) {
        return auditLogRepository.findByAccionOrderByFechaHoraDesc(accion);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String getEmailActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null && auth.getName() != null) ? auth.getName() : "sistema";
    }
}
