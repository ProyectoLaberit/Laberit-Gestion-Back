package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {

        List<DetalleEstimacion> findByIdExcel(Integer idExcel);

        List<DetalleEstimacion> findByIdTareaProyecto(Long idTareaProyecto);

        List<DetalleEstimacion> findByIdExcelAndIdTareaProyectoIn(Integer idExcel, List<Long> idTareaProyecto);

        DetalleEstimacion findFirstByIdExcelAndIdTareaProyecto(Integer idExcel, Long idTareaProyecto);

        long countByIdTareaProyecto(Long idTareaProyecto);

        // Consultas para el Excel Analítico
        @Query(value = "SELECT CAST(COALESCE(SUM(e.tiempo_max), 0.0) AS DOUBLE PRECISION) " +
                        "FROM detalle_estimacion e " +
                        "JOIN tarea_proyecto t ON e.id_tarea_proyecto = t.id_tarea_proyecto " +
                        "JOIN excel ex ON e.id_excel = ex.id_excel " +
                        "WHERE t.id_proyecto = :idProyecto AND ex.vigente = true", nativeQuery = true)
        Double obtenerTotalHorasMaximasProyecto(@Param("idProyecto") Long idProyecto);

        @Query(value = "SELECT CAST(COALESCE(SUM(e.tiempo_max), 0.0) AS DOUBLE PRECISION) " +
                        "FROM detalle_estimacion e " +
                        "JOIN tarea_proyecto t ON e.id_tarea_proyecto = t.id_tarea_proyecto " +
                        "WHERE t.id_proyecto = :idProyecto AND e.id_excel = :idExcel", nativeQuery = true)
        Double obtenerTotalHorasMaximasProyecto(@Param("idProyecto") Long idProyecto,
                        @Param("idExcel") Integer idExcel);

        @Query(value = "SELECT CAST(COALESCE(SUM(e.tiempo_min), 0.0) AS DOUBLE PRECISION) " +
                        "FROM detalle_estimacion e " +
                        "JOIN tarea_proyecto t ON e.id_tarea_proyecto = t.id_tarea_proyecto " +
                        "WHERE t.id_proyecto = :idProyecto AND e.id_excel = :idExcel", nativeQuery = true)
        Double obtenerTotalHorasMinimasProyecto(@Param("idProyecto") Long idProyecto,
                        @Param("idExcel") Integer idExcel);
}
