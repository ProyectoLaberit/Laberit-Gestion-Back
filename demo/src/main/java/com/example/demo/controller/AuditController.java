package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.AuditLog;
import com.example.demo.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    private AuditService auditService;

    // ── Guard de acceso ───────────────────────────────────────────────────────

    private boolean esSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPERADMINISTRADOR"));
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /** Todos los logs, ordenados del más reciente al más antiguo. */
    @GetMapping
    public ApiResponse obtenerTodos() {
        if (!esSuperAdmin()) {
            return new ApiResponse("Acceso denegado. Solo el SuperAdministrador puede ver los logs.", false, null);
        }
        List<AuditLog> logs = auditService.obtenerTodos();
        return new ApiResponse("Logs recuperados", true, logs);
    }

    /** Logs filtrados por proyecto. */
    @GetMapping("/proyecto/{idProyecto}")
    public ApiResponse obtenerPorProyecto(@PathVariable Long idProyecto) {
        if (!esSuperAdmin()) {
            return new ApiResponse("Acceso denegado.", false, null);
        }
        return new ApiResponse("Logs del proyecto", true, auditService.obtenerPorProyecto(idProyecto));
    }

    /** Logs filtrados por tipo de acción. */
    @GetMapping("/accion/{accion}")
    public ApiResponse obtenerPorAccion(@PathVariable String accion) {
        if (!esSuperAdmin()) {
            return new ApiResponse("Acceso denegado.", false, null);
        }
        return new ApiResponse("Logs por acción", true, auditService.obtenerPorAccion(accion));
    }

    /** Logs filtrados por usuario (email). */
    @GetMapping("/usuario/{email}")
    public ApiResponse obtenerPorUsuario(@PathVariable String email) {
        if (!esSuperAdmin()) {
            return new ApiResponse("Acceso denegado.", false, null);
        }
        return new ApiResponse("Logs del usuario", true, auditService.obtenerPorUsuario(email));
    }
}
