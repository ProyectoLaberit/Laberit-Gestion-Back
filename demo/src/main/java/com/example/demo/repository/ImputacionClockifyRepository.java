package com.example.demo.repository;

import com.example.demo.entity.ImputacionClockify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImputacionClockifyRepository extends JpaRepository<ImputacionClockify, Long> {

    // Suma de horas válidas: Filtra por la tarea y cuenta solo las que son válidas
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

    @Query("SELECT COALESCE(SUM(ic.horasTrabajadas), 0) FROM ImputacionClockify ic WHERE ic.idDetalleEstimacion = :idDetalle")
    Double sumarHorasPorDetalle(@Param("idDetalle") Long idDetalle);

    //COALESCE evita nulos
    //Suma horas totales en un proyecto
    @Query("SELECT COALESCE(SUM(i.horasTrabajadas), 0) FROM ImputacionClockify i WHERE i.idProyecto = :idProyecto AND i.valida = true")
    Double sumarHorasTotalesProyecto(@Param("idProyecto") Long idProyecto);

    // Devuelve una lista donde [0] es el ID de la tarea y [1] es la suma de sus horas válidas
    @Query("SELECT ic.idDetalleEstimacion, SUM(ic.horasTrabajadas) " +
        "FROM ImputacionClockify ic " +
        "WHERE ic.idProyecto = :idProyecto AND ic.valida = true " +
        "GROUP BY ic.idDetalleEstimacion")
    List<Object[]> sumarHorasValidasAgrupadasPorDetalle(@Param("idProyecto") Long idProyecto);

    // Devuelve las imputaciones filtradas por proyecto, subtarea y departamento
    List<ImputacionClockify> findByIdProyectoAndIdDetalleEstimacionAndIdDepartamento(Long idProyecto, Long idDetalleEstimacion, Integer idDepartamento);

    // Cuenta cuántas imputaciones válidas hay
    Integer countByIdProyectoAndIdDetalleEstimacionAndIdDepartamentoAndValidaTrue(Long idProyecto, Long idDetalleEstimacion, Integer idDepartamento);

    // Cuenta cuántas imputaciones inválidas hay
    Integer countByIdProyectoAndIdDetalleEstimacionAndIdDepartamentoAndValidaFalse(Long idProyecto, Long idDetalleEstimacion, Integer idDepartamento);
    
}