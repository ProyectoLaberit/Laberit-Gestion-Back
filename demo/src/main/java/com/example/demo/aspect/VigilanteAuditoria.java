package com.example.demo.aspect;

import com.example.demo.annotation.Auditable;
import com.example.demo.entity.AuditLog;
import com.example.demo.repository.AuditLogRepository;
import com.example.demo.services.AuditService;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.*;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.mapping.ManyToOne;
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
        Object entidadAntigua = null; // 1. Declarado aquí arriba

        // 1. INTENTAR SACAR LA "FOTO DEL ANTES"
        if (argumentos.length > 0 && argumentos[0] instanceof Number) {
            idAfectado = ((Number) argumentos[0]).longValue();
            
            entidadAntigua = entityManager.find(auditable.entidad(), idAfectado); // 2. Asignamos a la variable
            if (entidadAntigua != null) {
                // CORRECCIÓN: Se elimina el detach() para no romper la sesión de Hibernate
                jsonAntes = convertirAJson(entidadAntigua);
            }
        }

        // 2. EJECUCIÓN: Dejar que el método original se ejecute
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

            // --- MAGIA PARA LA DESCRIPCIÓN DINÁMICA (SpEL) ---
            String descripcionFinal = auditable.descripcion();

            if (!descripcionFinal.isEmpty() && descripcionFinal.contains("#{")) {
                try {
                    org.aspectj.lang.reflect.MethodSignature signature = (org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature();
                    String[] nombresParametros = signature.getParameterNames();

                    org.springframework.expression.spel.support.StandardEvaluationContext context = new org.springframework.expression.spel.support.StandardEvaluationContext();

                    // Mapear los parámetros de entrada (ej: dto)
                    for (int i = 0; i < argumentos.length; i++) {
                        context.setVariable(nombresParametros[i], argumentos[i]);
                    }

                    // Mapear el objeto devuelto (ej: resultado)
                    if (resultadoMetodo != null) {
                        context.setVariable("resultado", resultadoMetodo);
                    }

                    // 3. ¡AQUÍ ESTÁ LA INYECCIÓN DEL ANTIGUO!
                    if (entidadAntigua != null) {
                        context.setVariable("antiguo", entidadAntigua);
                    }

                    org.springframework.expression.ExpressionParser parser = new org.springframework.expression.spel.standard.SpelExpressionParser();
                    org.springframework.expression.ParserContext templateContext = new org.springframework.expression.common.TemplateParserContext();

                    // Parsear el texto
                    descripcionFinal = parser.parseExpression(descripcionFinal, templateContext).getValue(context, String.class);

                } catch (Exception e) {
                    System.err.println("[AUDITORÍA WARNING] No se pudo traducir la descripción dinámica: " + e.getMessage());
                }
            } else if (descripcionFinal.isEmpty()) {
                descripcionFinal = "Cambio realizado en la tabla " + auditable.tabla() + " (ID: " + idAfectado + ")";
            }
            
            log.setDescripcion(descripcionFinal);
            // --- FIN DE LA MAGIA ---

            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("[AUDITORÍA ERROR] No se pudo guardar el log: " + e.getMessage());
        }

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

        // Ignorar relaciones JPA automáticamente
        mapper.setSerializerFactory(
            mapper.getSerializerFactory().withSerializerModifier(
                new BeanSerializerModifier() {
                    @Override
                    public List<BeanPropertyWriter> changeProperties(
                            SerializationConfig config,
                            BeanDescription beanDesc,
                            List<BeanPropertyWriter> beanProperties) {

                        beanProperties.removeIf(writer -> {
                            var member = writer.getMember();
                            if (member == null) return false;

                            return member.hasAnnotation(ManyToOne.class)
                                    || member.hasAnnotation(OneToMany.class)
                                    || member.hasAnnotation(OneToOne.class)
                                    || member.hasAnnotation(ManyToMany.class);
                        });

                        return beanProperties;
                    }
                }
            )
        );

        return mapper.writeValueAsString(objeto);

    } catch (Exception e) {
        return "{\"error\": \"No se pudo serializar el objeto: " + e.getMessage() + "\"}";
    }
}

  
}