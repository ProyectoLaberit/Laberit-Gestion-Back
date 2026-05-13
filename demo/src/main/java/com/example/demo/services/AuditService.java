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

    // ==========================================
    // 1. HERRAMIENTA PARA EL VIGILANTE (AOP)
    // ==========================================

    /**
     * Extrae el ID del usuario logueado en este momento.
     * Si no hay nadie (ej. proceso automático), devuelve un ID por defecto (ej. 1).
     */
    public Integer getIdUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null && !auth.getName().equals("anonymousUser")) {
            return usuarioRepository.findByEmail(auth.getName())
                    .map(Usuario::getId)
                    .orElse(1); // 1 = ID del Sistema/Admin por defecto si falla
        }
        return 1; // Asumimos 1 (Sistema) si no hay contexto web
    }

    // ==========================================
    // 2. CONSULTAS PARA EL FRONT-END
    // ==========================================

    public List<AuditLog> obtenerTodos() {
        return auditLogRepository.findAll();
    }
}