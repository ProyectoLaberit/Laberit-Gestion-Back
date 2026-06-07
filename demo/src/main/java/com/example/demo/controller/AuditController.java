package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuditLogDTO;
import com.example.demo.services.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditController {

    @Autowired
    private AuditService auditService;

    

    /**
     * metodo para obtener todos las operaciones realizadas guardadas en la base de datos
     * @return ApiResponse json que contiene los logs almacenados en la base de datos
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPERADMINISTRADOR', 'ROLE_ADMINISTRADOR')")
    public ApiResponse obtenerTodos() {
       
        List<AuditLogDTO> logs = auditService.obtenerTodos();
        return new ApiResponse("Logs recuperados", true, logs);
    }
}
