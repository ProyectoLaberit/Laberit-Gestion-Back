package com.example.demo.aspect;

import com.example.demo.annotation.Auditable;
import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.services.AuditService;

// Importaciones explícitas del conversor JSON (Ya vienen incluidas en Spring Boot Web)
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class VigilanteAuditoria {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private EntityManager entityManager;

    @Around("@annotation(auditable)")
    public Object vigilarMetodo(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        
        Object[] argumentos = joinPoint.getArgs();
        String jsonAntes = null;
        Long idAfectado = null;

        // 1. INTENTAR SACAR LA "FOTO DEL ANTES"
        if (argumentos.length > 0 && argumentos[0] instanceof Number) {
            idAfectado = ((Number) argumentos[0]).longValue();
            
            Object entidadAntigua = entityManager.find(auditable.entidad(), idAfectado);
            if (entidadAntigua != null) {
                entityManager.detach(entidadAntigua);
                jsonAntes = convertirAJson(entidadAntigua);
            }
        }

        // 2. DEJAR QUE EL MÉTODO REAL SE EJECUTE
        Object resultadoMetodo = joinPoint.proceed();

        // 3. SACAR LA "FOTO DEL DESPUÉS"
        String jsonDespues = null;
        if (resultadoMetodo != null) {
            jsonDespues = convertirAJson(resultadoMetodo);
            
            if (idAfectado == null) {
                try {
                    Object idObj = resultadoMetodo.getClass().getMethod("getId").invoke(resultadoMetodo);
                    if (idObj instanceof Number) {
                        idAfectado = ((Number) idObj).longValue();
                    }
                } catch (Exception e) {
                    idAfectado = 0L;
                }
            }
        }

        // 4. GUARDAR EN LA BASE DE DATOS
        try {
            AuditLog log = new AuditLog();
            log.setIdUsuario(auditService.getIdUsuarioActual());
            log.setAccion(auditable.accion());
            log.setTablaAfectada(auditable.tabla());
            log.setIdAfectado(idAfectado != null ? idAfectado : 0L);
            log.setDatosPrevios(jsonAntes);
            log.setDatosNuevos(jsonDespues);
            
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[AUDITORÍA ERROR] No se pudo guardar el log: " + e.getMessage());
        }

        return resultadoMetodo;
    }

    // ==========================================
    // HERRAMIENTA BLINDADA DE CONVERSIÓN JSON
    // ==========================================
    private String convertirAJson(Object objeto) {
        if (objeto == null) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            // Soporte para fechas de Java 8 (LocalDateTime)
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            // Evitar que explote si Hibernate devuelve un proxy vacío
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            
            return mapper.writeValueAsString(objeto);
        } catch (Exception e) {
            return "{\"error\": \"No se pudo serializar el objeto: " + e.getMessage() + "\"}";
        }
    }
}