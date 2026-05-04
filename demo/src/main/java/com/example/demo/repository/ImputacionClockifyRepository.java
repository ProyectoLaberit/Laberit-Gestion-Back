package com.example.demo.repository;

import com.example.demo.entity.ImputacionClockify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImputacionClockifyRepository extends JpaRepository<ImputacionClockify, Long> {

    // Suma de horas válidas: Filtra por la tarea y cuenta SOLO AUTOMATICA y MANUAL
    @Query("SELECT COALESCE(SUM(i.horasTotales), 0) FROM ImputacionClockify i WHERE i.idDetalleEstimacion = :idDetalleEstimacion AND i.estado IN ('AUTOMATICA', 'MANUAL')")
    Double sumarHorasValidas(Long idDetalleEstimacion);

    // Devuelve las imputaciones ya validadas
    List<ImputacionClockify> findByIdDetalleEstimacionAndEstadoIn(Long idDetalleEstimacion, List<String> estados);

    // Devuelve las tareas huerfanas de esta tarea concreta
    List<ImputacionClockify> findByIdDetalleEstimacionAndEstado(Long idDetalleEstimacion, String estado);
}