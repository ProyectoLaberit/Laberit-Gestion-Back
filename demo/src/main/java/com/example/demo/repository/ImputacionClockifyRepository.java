package com.example.demo.repository;

import com.example.demo.entity.ImputacionClockify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImputacionClockifyRepository extends JpaRepository<ImputacionClockify, Long> {

    // Suma de horas válidas: Filtra por la tarea y cuenta SOLO AUTOMATICA y MANUAL
    @Query("SELECT COALESCE(SUM(i.horasTrabajadas), 0) FROM ImputacionClockify i WHERE i.idDetalleEstimacion = :idDetalleEstimacion AND i.valida = true")
    Double sumarHorasValidas(@Param("idDetalleEstimacion") Long idDetalleEstimacion);

   // Devuelve las imputaciones ya validadas de una tarea
    List<ImputacionClockify> findByIdDetalleEstimacionAndValidaTrue(Long idDetalleEstimacion);

    // Devuelve las tareas huerfanas (no válidas) de un proyecto completo
    List<ImputacionClockify> findByIdProyectoAndValidaFalse(Long idProyecto);

    List<ImputacionClockify> findByIdProyecto(Long idProyecto);
    
    List<ImputacionClockify> findByIdProyectoAndValida(Long idProyecto, Boolean valida);
    
    boolean existsByIdClockifyOriginal(String idClockifyOriginal);

     ImputacionClockify findByIdClockifyOriginal(String idClockifyOriginal);

    @Query("SELECT SUM(ic.horasTrabajadas) FROM ImputacionClockify ic WHERE ic.idDetalleEstimacion = :idDetalle")
    Double sumarHorasPorDetalle(@Param("idDetalle") Long idDetalle);

}