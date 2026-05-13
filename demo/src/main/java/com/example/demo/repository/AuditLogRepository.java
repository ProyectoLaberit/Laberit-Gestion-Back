package com.example.demo.repository;

import com.example.demo.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

   // Devuelve todos los logs ordenados por fecha (el más nuevo primero)
    List<AuditLog> findAllByOrderByFechaHoraDesc();
    
    // Buscar por el tipo de acción (ej. "ACTUALIZAR_USUARIO")
    List<AuditLog> findByAccionOrderByFechaHoraDesc(String accion);
    
    // Buscar por la tabla afectada (ej. "usuario")
    List<AuditLog> findByTablaAfectadaOrderByFechaHoraDesc(String tablaAfectada);
    
    // Buscar todas las acciones que hizo un usuario en concreto
    List<AuditLog> findByIdUsuarioOrderByFechaHoraDesc(Integer idUsuario);
}
