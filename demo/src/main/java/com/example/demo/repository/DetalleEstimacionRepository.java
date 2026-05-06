package com.example.demo.repository;

import com.example.demo.entity.DetalleEstimacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface DetalleEstimacionRepository extends JpaRepository<DetalleEstimacion, Long> {
    
    List<DetalleEstimacion> findByIdExcel(Integer idExcel);

    DetalleEstimacion findFirstByIdExcelAndIdFaseAndTareaIgnoreCase(Integer idExcel, Integer idFase, String tarea);

    // NUEVO MÉTODO: Busca de forma segura cruzando el Excel actual y el ID del Issue
    DetalleEstimacion findFirstByIdExcelAndNumeroGitlab(Integer idExcel, String numeroGitlab);
    
}