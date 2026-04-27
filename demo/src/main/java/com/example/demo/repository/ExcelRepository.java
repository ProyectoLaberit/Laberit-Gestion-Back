package com.example.demo.repository;


import com.example.demo.dto.HistorialExcelDTO;
import com.example.demo.entity.Excel;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcelRepository extends JpaRepository<Excel, Integer> {

   Excel findTopByIdProyectoOrderByIdExcelDesc(Long idProyecto);

    Excel findFirstByIdProyectoAndVigenteTrue(Long idProyecto);

    /* 
    Método para desactivar excels anteriores de un proyecto 
    */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Excel e SET e.vigente = false WHERE e.idProyecto = :idProyecto")
    void desactivarExcelsAnteriores(@Param("idProyecto") Long idProyecto);
    
    /*
    Método para obtener el historial de excels de un proyecto, ordenados por fecha de subida (más reciente primero).
     * Devuelve una lista de HistorialExcelDTO con la información necesaria para mostrar en el frontend.
     * Se asume que el DTO tiene campos como idExcel, fechaSubida y nombreUsuario.
     * El query se puede ajustar según la estructura real de tus entidades y relaciones.
    */@Query("SELECT new com.example.demo.dto.HistorialExcelDTO(e.idExcel, e.fechaSubida, u.nombre, e.vigente) " +
       "FROM Excel e, Usuario u " +
       "WHERE e.idUsuario = u.id AND e.idProyecto = :proyectoId " +
       "ORDER BY e.fechaSubida DESC")
List<HistorialExcelDTO> obtenerHistorialDirecto(@Param("proyectoId") Long proyectoId);
}

