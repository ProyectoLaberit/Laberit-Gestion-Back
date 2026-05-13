package com.example.demo.aspect;

import com.example.demo.annotation.Auditable;
import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.services.AuditService;
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

    /**
     * Intercepta métodos marcados con @Auditable para registrar cambios.
     */
    @Around("@annotation(auditable)")
    public Object vigilarMetodo(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        System.out.println("¡EL VIGILANTE HA INTERCEPTADO EL MÉTODO: " + auditable.accion() + "!");
        
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

        // 2. EJECUCIÓN: Dejar que el método original se ejecute
        // Guardamos el resultado para devolverlo al final
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

            String descripcionFinal = auditable.descripcion().isEmpty() 
                ? "Cambio realizado en la tabla " + auditable.tabla() + " (ID: " + idAfectado + ")"
                : auditable.descripcion();
            
            log.setDescripcion(descripcionFinal);

            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[AUDITORÍA ERROR] No se pudo guardar el log: " + e.getMessage());
        }

        // CRÍTICO: Devolvemos el resultado del método original para que el controlador funcione
        return resultadoMetodo; 
    }

    /**
     * Herramienta blindada para convertir entidades a JSON.
     */
    private String convertirAJson(Object objeto) {
        if (objeto == null) {
            return null;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
            
            return mapper.writeValueAsString(objeto);
        } catch (Exception e) {
            return "{\"error\": \"No se pudo serializar el objeto: " + e.getMessage() + "\"}";
        }
    }
}