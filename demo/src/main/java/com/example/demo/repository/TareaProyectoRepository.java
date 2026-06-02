package com.example.demo.repository;

import com.example.demo.dto.excel.FilaComparativaDTO;
import com.example.demo.entity.TareaProyecto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface TareaProyectoRepository extends JpaRepository<TareaProyecto, Long> {
    
    Optional<TareaProyecto> findByIdProyectoAndIdFaseAndIdDepartamentoAndTarea(
            Long idProyecto, Integer idFase, Integer idDepartamento, String tarea
    );

    // método para sacar todas las tareas de un proyecto
    List<TareaProyecto> findByIdProyecto(Long idProyecto);

    List<TareaProyecto> findByTarea(String tarea);

    List<TareaProyecto> findByIdProyectoAndIdFaseAndTarea(Long idProyecto, Integer idFase, String tarea);

    @Query(value = "SELECT t.* FROM tarea_proyecto t " +
           "LEFT JOIN imputacion_clockify i ON t.id_tarea_proyecto = i.id_tarea_proyecto " +
           "WHERE i.id_tarea_proyecto IS NULL", nativeQuery = true)
    List<TareaProyecto> findTareasSinImputacionClockify();

    @Query(value = "SELECT t.* FROM tarea_proyecto t " +
           "LEFT JOIN imputacion_clockify i ON t.id_tarea_proyecto = i.id_tarea_proyecto " +
           "WHERE i.id_tarea_proyecto IS NULL AND t.id_proyecto = :idProyecto", nativeQuery = true)
    List<TareaProyecto> findTareasSinImputacionClockifyByProyecto(@Param("idProyecto") Long idProyecto);

    ///Concultas para el Excel Analítico
    

    @Query(value = "SELECT " +
            "e.id_excel AS idExcel, " +
            "CAST(g.numero_gitlab AS VARCHAR) AS idGitlab, " + 
            "COALESCE(fp.nombre, f.nombre) AS fase, " +  
            "t.tarea AS tarea, " +
            "d.nombre AS departamento, " +
            "COALESCE(e.tiempo_min, 0.0) AS estimacionMinima, " +
            "COALESCE(e.tiempo_max, 0.0) AS estimacionMaxima, " +
            "COALESCE(SUM(c.horas_trabajadas), 0.0) AS horasReales, " +
            "(COALESCE(SUM(c.horas_trabajadas), 0.0) - COALESCE(e.tiempo_max, 0.0)) AS desviacionHoras, " +
            "CASE WHEN COALESCE(e.tiempo_max, 0.0) > 0 THEN ((COALESCE(SUM(c.horas_trabajadas), 0.0) - e.tiempo_max) / e.tiempo_max) * 100 ELSE 0.0 END AS desviacionPorcentaje, " +
            "COALESCE(g.estado, 'Sin issue') AS estadoGitlab " +
            "FROM tarea_proyecto t " +
            "LEFT JOIN fase f ON t.id_fase = f.id_fase " +
            "LEFT JOIN fase fp ON f.fase_padre = fp.id_fase " + 
            "LEFT JOIN departamento d ON t.id_departamento = d.id_departamento " +
            "LEFT JOIN detalle_estimacion e ON e.id_tarea_proyecto = t.id_tarea_proyecto " +
            "LEFT JOIN excel ex ON e.id_excel = ex.id_excel AND ex.vigente = true " +
            "LEFT JOIN imputacion_clockify c ON c.id_tarea_proyecto = t.id_tarea_proyecto AND c.valida = true " +
            "LEFT JOIN tarea_gitlab g ON g.id_tarea_proyecto = t.id_tarea_proyecto AND g.valida = true " +
            "WHERE t.id_proyecto = :idProyecto " +
            "GROUP BY t.id_tarea_proyecto, e.id_excel, g.numero_gitlab, fp.nombre, f.nombre, t.tarea, d.nombre, e.tiempo_min, e.tiempo_max, g.estado", 
            nativeQuery = true)
    List<FilaComparativaDTO> obtenerComparativaTareas(@Param("idProyecto") Long idProyecto);

    int countByIdProyecto(Long idProyecto);
}
