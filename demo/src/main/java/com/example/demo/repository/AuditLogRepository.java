package com.example.demo.repository;

import com.example.demo.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Todos los logs ordenados por fecha desc
    List<AuditLog> findAllByOrderByFechaHoraDesc();

    // Logs de un proyecto concreto
    List<AuditLog> findByIdProyectoOrderByFechaHoraDesc(Long idProyecto);

    // Logs de un usuario concreto
    List<AuditLog> findByUsuarioEmailOrderByFechaHoraDesc(String email);

    // Logs de un usuario concreto por ID
    List<AuditLog> findByIdUsuarioOrderByFechaHoraDesc(Integer idUsuario);

    // Logs de un tipo de acción concreto
    List<AuditLog> findByAccionOrderByFechaHoraDesc(String accion);
}
