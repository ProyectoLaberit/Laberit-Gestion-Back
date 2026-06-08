package com.example.demo.repository;

import com.example.demo.dto.excel.ErrorVinculacionClockifyDTO;
import com.example.demo.entity.ImputacionClockify;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImputacionClockifyRepository extends JpaRepository<ImputacionClockify, Long> {

        // Suma de horas: Busca el numero de GitLab de esta tarea, si lo tiene suma sus imputaciones, si no devuelve 0
        @Query("SELECT COALESCE(SUM(ic.horasTrabajadas), 0) " +
                "FROM ImputacionClockify ic " +
                "JOIN GitLabTarea gt ON ic.numeroGitlab = gt.numeroGitlab AND ic.idProyecto = gt.idProyecto " +
                "WHERE gt.tareaProyecto = :idTareaProyecto AND ic.valida = true")
        Double sumarHorasValidas(@Param("idTareaProyecto") Long idTareaProyecto);

        // Devuelve las imputaciones ya validadas de una tarea
        List<ImputacionClockify> findByIdTareaProyectoAndValidaTrue(Long idTareaProyecto);

        // Devuelve las tareas huerfanas (no válidas) de un proyecto completo
        List<ImputacionClockify> findByIdProyectoAndValidaFalse(Long idProyecto);

        List<ImputacionClockify> findByIdProyecto(Long idProyecto);

        List<ImputacionClockify> findByIdTareaProyectoIn(List<Long> idsTareaProyecto);

        List<ImputacionClockify> findByIdProyectoAndValida(Long idProyecto, Boolean valida);

        boolean existsByIdClockifyOriginal(String idClockifyOriginal);

        @Query("SELECT i FROM ImputacionClockify i WHERE i.idClockifyOriginal = :idClockifyOriginal")
        ImputacionClockify findByIdClockifyOriginal(String idClockifyOriginal);

        @Query("SELECT COALESCE(SUM(ic.horasTrabajadas), 0) " +
                "FROM ImputacionClockify ic " +
                "JOIN GitLabTarea gt ON ic.numeroGitlab = gt.numeroGitlab AND ic.idProyecto = gt.idProyecto " +
                "WHERE gt.tareaProyecto = :idTareaProyecto AND ic.valida = true")
        Double sumarHorasPorTarea(@Param("idTareaProyecto") Long idTareaProyecto);


        // Suma horas totales en un proyecto, asegurando que la imputación pertenece a un issue de GitLab asignado en el proyecto
        @Query("SELECT COALESCE(SUM(ic.horasTrabajadas), 0) " +
                "FROM ImputacionClockify ic " +
                "WHERE ic.idProyecto = :idProyecto AND ic.valida = true " +
                "AND ic.numeroGitlab IN (SELECT gt.numeroGitlab FROM GitLabTarea gt WHERE gt.idProyecto = :idProyecto AND gt.tareaProyecto IS NOT NULL)")
        Double sumarHorasTotalesProyecto(@Param("idProyecto") Long idProyecto);

        @Query("SELECT COALESCE(SUM(ic.horasTrabajadas), 0) " +
                "FROM ImputacionClockify ic " +
                "WHERE ic.idProyecto = :idProyecto AND ic.valida = true " +
                "AND ic.idTareaProyecto IN (" +
                "       SELECT d.idTareaProyecto FROM DetalleEstimacion d WHERE d.idExcel = :idExcel" +
                ")")
        Double sumarHorasTotalesProyectoPorExcel(@Param("idProyecto") Long idProyecto,
                @Param("idExcel") Integer idExcel);

        // Devuelve una lista donde [0] es el ID de la tarea y [1] es la suma de sus
        // horas válidas
        @Query("SELECT gt.tareaProyecto, SUM(ic.horasTrabajadas) " +
                "FROM ImputacionClockify ic " +
                "JOIN GitLabTarea gt ON ic.numeroGitlab = gt.numeroGitlab AND ic.idProyecto = gt.idProyecto " +
                "WHERE ic.idProyecto = :idProyecto AND ic.valida = true AND gt.tareaProyecto IS NOT NULL " +
                "GROUP BY gt.tareaProyecto")
        List<Object[]> sumarHorasValidasAgrupadasPorTarea(@Param("idProyecto") Long idProyecto);

        // Devuelve las imputaciones filtradas por proyecto, subtarea y departamento
        // List<ImputacionClockify>
        // findByIdProyectoAndIdDetalleEstimacionAndIdDepartamento(Long idProyecto, Long
        // idDetalleEstimacion, Integer idDepartamento);

        // Cuenta cuántas imputaciones válidas hay
        Integer countByIdProyectoAndIdTareaProyectoAndIdDepartamentoAndValidaTrue(Long idProyecto, Long idTareaProyecto,
                Integer idDepartamento);

        // Cuenta cuántas imputaciones inválidas hay
        Integer countByIdProyectoAndIdTareaProyectoAndIdDepartamentoAndValidaFalse(Long idProyecto, Long idTareaProyecto,
                Integer idDepartamento);

        // Filtrar imputaciones por departamento, tarea y un rango de fechas
        // List<ImputacionClockify>
        // findByIdProyectoAndIdDetalleEstimacionAndIdDepartamentoAndFechaBetween(Long
        // idProyecto, Long idDetalleEstimacion, Integer idDepartamento,
        // java.time.LocalDate desde, java.time.LocalDate hasta);

        // Devuelve las válidas de la tarea + TODAS las huérfanas de ese departamento
        @Query("SELECT i FROM ImputacionClockify i WHERE i.idProyecto = :idProyecto AND i.idDepartamento = :idDepartamento AND (i.idTareaProyecto = :idTareaProyecto OR (i.valida = false AND i.idTareaProyecto IS NULL))")
        List<ImputacionClockify> obtenerDatosVistaDepartamento(@Param("idProyecto") Long idProyecto,
                @Param("idTareaProyecto") Long idTareaProyecto, @Param("idDepartamento") Integer idDepartamento);

        // Lo mismo, pero aplicando el filtro de fechas
        @Query("SELECT i FROM ImputacionClockify i WHERE i.idProyecto = :idProyecto AND i.idDepartamento = :idDepartamento AND (i.idTareaProyecto = :idTareaProyecto OR (i.valida = false AND i.idTareaProyecto IS NULL)) AND i.fecha BETWEEN :desde AND :hasta")
        List<ImputacionClockify> obtenerDatosVistaDepartamentoFechas(@Param("idProyecto") Long idProyecto,
                @Param("idTareaProyecto") Long idTareaProyecto, @Param("idDepartamento") Integer idDepartamento,
                @Param("desde") java.time.LocalDate desde, @Param("hasta") java.time.LocalDate hasta);

        // Consulta para el Excel Analítico: Suma de horas válidas por tarea, agrupada
        // por proyecto

        @Query(value = "SELECT * FROM imputacion_clockify " +
                "WHERE id_proyecto = :idProyecto AND valida = true " +
                "ORDER BY fecha ASC", nativeQuery = true)
        List<ImputacionClockify> obtenerImputacionesValidasOrdenadas(@Param("idProyecto") Long idProyecto);

        @Query(value = "SELECT " +
                "'-' AS usuario, " +
                "c.descripcion_original AS descripcionOriginal, " +
                "c.fecha AS fechaTrabajada, " +
                "c.horas_trabajadas AS horasTrabajadas, " +
                "c.valida AS valida " +
                "FROM imputacion_clockify c " +
                "WHERE c.id_proyecto = :idProyecto AND c.valida = false", nativeQuery = true)
        List<ErrorVinculacionClockifyDTO> obtenerErroresClockify(@Param("idProyecto") Long idProyecto);

        Integer countByIdProyectoAndValidaFalse(Long idProyecto);

        List<ImputacionClockify> findByNumeroGitlabAndIdProyecto(Long numeroGitlab, Long idProyecto);

        List<ImputacionClockify> findByNumeroGitlabAndIdProyectoAndFechaBetween(Long numeroGitlab, Long idProyecto,
                java.time.LocalDate desde, java.time.LocalDate hasta);
}
