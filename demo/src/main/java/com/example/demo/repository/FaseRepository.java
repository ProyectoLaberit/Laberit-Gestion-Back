package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.Fase;

@Repository
public interface FaseRepository extends JpaRepository<Fase, Integer> {
    
    @Query("SELECT f FROM Fase f WHERE f.id IN (SELECT DISTINCT d.idFase FROM DetalleEstimacion d WHERE d.idExcel = :idExcel)")
    List<Fase> findSubfasesConTareasPorExcel(@Param("idExcel") Integer idExcel);
}