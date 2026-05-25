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

    //Consultas para el Excel Analítico
    @Query("SELECT COALESCE(SUM(e.tiempoMax), 0.0) FROM DetalleEstimacion e " +
           "WHERE e.tareaProyecto.proyecto.id = :idProyecto AND e.excel.vigente = true")
    Double obtenerTotalHorasMaximasProyecto(@Param("idProyecto") Long idProyecto);
}
