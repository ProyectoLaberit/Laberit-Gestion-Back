package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuditLogDTO;
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

    /**
     * Metodo que comprueba si el usuario que envia la pregunta tiene el rol de superadministrador
     * @return Boolean true si es un superadmin, false si no lo es
     */
    private boolean esSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_SUPERADMINISTRADOR"));
    }

    /**
     * metodo para obtener todos las operaciones realizadas guardadas en la base de datos
     * @return ApiResponse json que contiene los logs almacenados en la base de datos
     */
    @GetMapping
    public ApiResponse obtenerTodos() {
        if (!esSuperAdmin()) {
            return new ApiResponse("Acceso denegado. Solo el SuperAdministrador puede ver los logs.", false, null);
        }
        List<AuditLogDTO> logs = auditService.obtenerTodos();
        return new ApiResponse("Logs recuperados", true, logs);
    }
}
