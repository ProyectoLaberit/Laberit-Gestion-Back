package com.example.demo.repository;


import com.example.demo.entity.Excel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcelRepository extends JpaRepository<Excel, Integer> {

   Excel findTopByIdProyectoOrderByIdExcelDesc(Long idProyecto);

    Excel findFirstByIdProyectoAndVigenteTrue(Long idProyecto);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Excel e SET e.vigente = false WHERE e.idProyecto = :idProyecto")
    void desactivarExcelsAnteriores(@Param("idProyecto") Long idProyecto);
}