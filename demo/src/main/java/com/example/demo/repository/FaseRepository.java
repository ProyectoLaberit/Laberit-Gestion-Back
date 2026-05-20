package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Fase;

@Repository
public interface FaseRepository extends JpaRepository<Fase, Integer> {
    
    @Query("SELECT f FROM Fase f WHERE f.id IN (SELECT DISTINCT tp.idFase FROM DetalleEstimacion d, TareaProyecto tp WHERE d.idTareaProyecto = tp.id AND d.idExcel = :idExcel)")
    List<Fase> findSubfasesConTareasPorExcel(@Param("idExcel") Integer idExcel);
}